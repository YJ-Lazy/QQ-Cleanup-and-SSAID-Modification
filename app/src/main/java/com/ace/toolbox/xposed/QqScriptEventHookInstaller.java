package com.ace.toolbox.xposed;

import android.util.Log;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/**
 * QQ NT event bridge for BeanShell callbacks.
 *
 * QFun's public implementation identifies QQ's message service with DexKit and hooks
 * `onRecvMsg` / `onAddSendMsg`. ACE independently uses a filtered class-load watcher so it can
 * discover the same stable method names without bundling DexKit.
 *
 * Group processors vary more between QQ builds. Known/semantic push processor class names are
 * detected and exposed through the generic onGroupEvent callback. The exact join processor used
 * by current QQ NT is also explicitly probed.
 */
final class QqScriptEventHookInstaller {
    private static final String TAG = "ACE-ScriptEvent";

    private static final Set<String> INSPECTED =
            Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> HOOKED =
            Collections.synchronizedSet(new HashSet<>());
    private static final ThreadLocal<Boolean> INSPECT_GUARD = new ThreadLocal<>();

    static void install(XposedModule module, ClassLoader hostLoader) {
        if (hostLoader == null) return;

        probeKnownClasses(module, hostLoader);
        installClassLoadWatcher(module);
        module.log(Log.INFO, TAG,
                "QQ script event discovery installed (message/group callbacks are config-gated)");
    }

    private static void probeKnownClasses(XposedModule module, ClassLoader loader) {
        String[] known = {
                "com.tencent.qqnt.push.processor.TroopMemberAddPushProcessor",
                "com.tencent.qqnt.push.processor.TroopMemberDeletePushProcessor",
                "com.tencent.qqnt.push.processor.TroopMemberRemovePushProcessor",
                "com.tencent.qqnt.push.processor.TroopMemberMutePushProcessor",
                "com.tencent.qqnt.push.processor.TroopShutUpPushProcessor"
        };
        for (String name : known) {
            try {
                Class<?> c = Class.forName(name, false, loader);
                inspectClass(module, c);
            } catch (Throwable ignored) {}
        }
    }

    private static void installClassLoadWatcher(XposedModule module) {
        String key = "classloader-watch";
        if (!HOOKED.add(key)) return;

        try {
            Method load = ClassLoader.class.getDeclaredMethod(
                    "loadClass", String.class, boolean.class);
            load.setAccessible(true);
            module.hook(load)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (result instanceof Class && !Boolean.TRUE.equals(INSPECT_GUARD.get())) {
                            String name = String.valueOf(chain.getArg(0));
                            if (interestingClassName(name)) {
                                try {
                                    INSPECT_GUARD.set(Boolean.TRUE);
                                    inspectClass(module, (Class<?>) result);
                                } finally {
                                    INSPECT_GUARD.remove();
                                }
                            }
                        }
                        return result;
                    });
            module.log(Log.INFO, TAG, "Filtered ClassLoader watcher installed");
        } catch (Throwable t) {
            module.log(Log.ERROR, TAG, "Unable to install class-load watcher", t);
        }
    }

    private static boolean interestingClassName(String name) {
        if (name == null) return false;
        return name.startsWith("com.tencent.qqnt.msg")
                || name.startsWith("com.tencent.qqnt.push.processor")
                || name.startsWith("com.tencent.mobileqq.troop");
    }

    private static void inspectClass(XposedModule module, Class<?> cls) {
        if (cls == null) return;
        String className = cls.getName();
        if (!INSPECTED.add(className)) return;

        if (className.startsWith("com.tencent.qqnt.msg")) {
            hookMessageMethods(module, cls);
        }

        String groupType = groupTypeForClass(className);
        if (groupType != null) {
            hookGroupProcessor(module, cls, groupType);
        }
    }

    private static void hookMessageMethods(XposedModule module, Class<?> cls) {
        boolean found = false;
        for (Method method : safeDeclaredMethods(cls)) {
            String name = method.getName();
            if (!"onRecvMsg".equals(name) && !"onAddSendMsg".equals(name)) continue;
            if (method.getParameterTypes().length < 1) continue;

            String hookKey = "msg:" + signature(method);
            if (!HOOKED.add(hookKey)) continue;
            try {
                method.setAccessible(true);
                boolean receive = "onRecvMsg".equals(name);
                module.hook(method)
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            try {
                                Object arg0 = chain.getArg(0);
                                if (receive) {
                                    Object record = firstRecord(arg0);
                                    if (record != null) HostJavaScriptEngine.dispatchReceive(record);
                                } else {
                                    Object record = firstRecord(arg0);
                                    if (record != null) HostJavaScriptEngine.dispatchSend(record);
                                }
                            } catch (Throwable t) {
                                Log.e(TAG, "Message event dispatch failed", t);
                            }
                            return result;
                        });
                found = true;
                module.log(Log.INFO, TAG,
                        (receive ? "Receive" : "Send")
                                + " callback hook installed: " + signature(method));
            } catch (Throwable t) {
                module.log(Log.ERROR, TAG, "Message hook failed: " + signature(method), t);
            }
        }
        if (found) {
            module.log(Log.INFO, TAG, "QQ message service discovered: " + cls.getName());
        }
    }

    private static Object firstRecord(Object value) {
        if (value == null) return null;
        if (value instanceof Collection) {
            Collection<?> c = (Collection<?>) value;
            return c.isEmpty() ? null : c.iterator().next();
        }
        if (value.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(value) == 0
                    ? null
                    : java.lang.reflect.Array.get(value, 0);
        }
        return value;
    }

    private static String groupTypeForClass(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (!(lower.contains("troop") || lower.contains("group"))) return null;

        if (lower.contains("memberadd") || lower.contains("memberjoin")
                || lower.contains("joinpush")) {
            return "join";
        }
        if (lower.contains("memberdelete") || lower.contains("memberremove")
                || lower.contains("memberquit") || lower.contains("leavepush")) {
            return "quit";
        }
        if (lower.contains("shutup") || lower.contains("mute")
                || lower.contains("forbid")) {
            return "shutup";
        }
        return null;
    }

    private static void hookGroupProcessor(
            XposedModule module,
            Class<?> cls,
            String type
    ) {
        for (Method method : safeDeclaredMethods(cls)) {
            if (Modifier.isStatic(method.getModifiers())
                    || method.isSynthetic()
                    || method.getParameterTypes().length == 0
                    || method.getParameterTypes().length > 6) {
                continue;
            }

            // Push processors normally expose a small void handler taking ArrayList<Byte>,
            // byte[] or IDs. Restricting to void avoids hooking getters/helpers.
            if (method.getReturnType() != void.class) continue;

            boolean plausible = false;
            for (Class<?> t : method.getParameterTypes()) {
                if (Collection.class.isAssignableFrom(t)
                        || t == byte[].class
                        || t == String.class
                        || CharSequence.class.isAssignableFrom(t)
                        || Number.class.isAssignableFrom(t)
                        || t.isPrimitive()) {
                    plausible = true;
                }
            }
            if (!plausible) continue;

            String key = "group:" + type + ":" + signature(method);
            if (!HOOKED.add(key)) continue;
            try {
                method.setAccessible(true);
                module.hook(method)
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            try {
                                Object[] args = new Object[method.getParameterCount()];
                                for (int i = 0; i < args.length; i++) args[i] = chain.getArg(i);
                                HostJavaScriptEngine.dispatchGroup(
                                        AceScriptGroupEvent.bestEffort(type, args));
                            } catch (Throwable t) {
                                Log.e(TAG, "Group event dispatch failed", t);
                            }
                            return result;
                        });
                module.log(Log.INFO, TAG,
                        "Group " + type + " callback hook installed: " + signature(method));
            } catch (Throwable t) {
                module.log(Log.ERROR, TAG,
                        "Group hook failed: " + signature(method), t);
            }
        }
    }

    private static Method[] safeDeclaredMethods(Class<?> cls) {
        try {
            return cls.getDeclaredMethods();
        } catch (Throwable ignored) {
            return new Method[0];
        }
    }

    private static String signature(Method m) {
        StringBuilder b = new StringBuilder(m.getDeclaringClass().getName())
                .append('#').append(m.getName()).append('(');
        Class<?>[] p = m.getParameterTypes();
        for (int i = 0; i < p.length; i++) {
            if (i > 0) b.append(',');
            b.append(p[i].getName());
        }
        return b.append(')').toString();
    }

    private QqScriptEventHookInstaller() {}
}
