package com.ace.toolbox.xposed;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import io.github.libxposed.api.XposedModule;

/**
 * Modern libxposed API 102 entry.
 *
 * v0.2.0 intentionally logs through XposedInterface.log(), not only android.util.Log,
 * so the framework module log can prove whether ACE is loaded before any QQ UI hook runs.
 */
public final class ModuleMain extends XposedModule {
    private static final String TAG = "ACE";

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        log(Log.INFO, TAG,
                "ACE 0.3.6 loaded; process=" + param.getProcessName()
                        + "; api=" + getApiVersion()
                        + "; framework=" + getFrameworkName() + " " + getFrameworkVersion());
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        String pkg = param.getPackageName();
        if (!HostPackages.QQ.equals(pkg) && !HostPackages.WECHAT.equals(pkg)) return;

        log(Log.INFO, TAG,
                "onPackageLoaded pkg=" + pkg
                        + "; first=" + param.isFirstPackage()
                        + "; loader=" + param.getDefaultClassLoader());

        // Install the Activity lifecycle watcher as early as possible. This does not depend on
        // QQ's Application subclass or AppComponentFactory and is enough to discover the settings UI.
        HostHookInstaller.installEarly(this, pkg);
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        String pkg = param.getPackageName();
        if (!HostPackages.QQ.equals(pkg) && !HostPackages.WECHAT.equals(pkg)) return;

        log(Log.INFO, TAG,
                "onPackageReady pkg=" + pkg + "; loader=" + param.getClassLoader());
        HostHookInstaller.install(this, param);
    }
}
