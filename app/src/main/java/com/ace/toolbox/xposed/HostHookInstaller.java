package com.ace.toolbox.xposed;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

final class HostHookInstaller {
    private static final String TAG = "ACE-Hook";
    private static final Set<String> INSTALLED = Collections.synchronizedSet(new HashSet<>());

    static void install(XposedModule module, XposedModule.PackageReadyParam param) {
        String pkg = param.getPackageName();
        ClassLoader loader = param.getClassLoader();
        try {
            String appClassName = param.getApplicationInfo().className;
            Class<?> appClass = appClassName == null || appClassName.isEmpty()
                    ? Application.class : Class.forName(appClassName, false, loader);
            Method onCreate = ReflectionUtils.findMethod(appClass, "onCreate");
            if (onCreate == null) onCreate = Application.class.getDeclaredMethod("onCreate");
            onCreate.setAccessible(true);
            module.hook(onCreate)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object self = chain.getThisObject();
                        if (self instanceof Application) {
                            Application app = (Application) self;
                            installForApplication(module, app, pkg, loader);
                        }
                        return result;
                    });
        } catch (Throwable t) {
            Log.e(TAG, "Unable to install Application bootstrap for " + pkg, t);
            HookRule rule = RuleRepository.loadBundled(pkg);
            if (rule != null) installSettingHooks(module, pkg, loader, rule);
        }
    }

    private static void installForApplication(XposedModule module, Application app, String pkg, ClassLoader loader) {
        String key = pkg + "@" + System.identityHashCode(loader);
        if (!INSTALLED.add(key)) return;

        HookRule rule = RuleRepository.resolveCachedOrBundled(app, pkg);
        if (rule != null) installSettingHooks(module, pkg, loader, rule);

        if (HostPackages.QQ.equals(pkg)) {
            SsaidFeature.install(module, app, loader);
        }

        new Thread(() -> {
            HookRule remote = RuleRepository.fetchRemote(app, pkg);
            if (remote != null) installSettingHooks(module, pkg, loader, remote);
        }, "ACE-rule-refresh").start();
    }

    private static void installSettingHooks(XposedModule module, String pkg, ClassLoader loader, HookRule rule) {
        for (String className : rule.settingClasses) {
            String hookKey = pkg + ":" + className;
            if (!INSTALLED.add(hookKey)) continue;
            try {
                Class<?> cls = Class.forName(className, false, loader);
                Method method = ReflectionUtils.findMethod(cls, rule.methodName, Bundle.class);
                if (method == null) {
                    Log.w(TAG, "No " + rule.methodName + "(Bundle): " + className);
                    continue;
                }
                method.setAccessible(true);
                module.hook(method)
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            Object self = chain.getThisObject();
                            if (self instanceof Activity && self.getClass().getName().equals(className)) {
                                Activity activity = (Activity) self;
                                activity.getWindow().getDecorView().post(() -> HostSettingInjector.inject(activity, pkg));
                            }
                            return result;
                        });
                Log.i(TAG, "Installed settings hook: " + className);
            } catch (Throwable t) {
                Log.w(TAG, "Setting candidate unavailable: " + className + " / " + t.getMessage());
            }
        }
    }
}
