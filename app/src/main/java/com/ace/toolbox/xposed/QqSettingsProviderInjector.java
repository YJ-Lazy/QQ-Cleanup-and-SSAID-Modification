package com.ace.toolbox.xposed;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/**
 * QQ NT native settings-entry injector.
 *
 * Instead of guessing the rendered View hierarchy, this hooks QQ's settings configuration
 * provider and adds one item to the data model that QQ itself renders. This is much closer to
 * how mature QQ modules expose a settings entry, while keeping ACE's implementation independent.
 */
final class QqSettingsProviderInjector {
    private static final String TAG = "ACE-QQSettings";
    private static final int ENTRY_ID = 0x5ACE0201;

    private static final String[] PROVIDERS = {
            "com.tencent.mobileqq.setting.main.MainSettingConfigProvider",
            "com.tencent.mobileqq.setting.main.NewSettingConfigProvider",
            "com.tencent.mobileqq.setting.main.b"
    };

    private static final String[] BASE_PROCESSOR_HINTS = {
            "com.tencent.mobileqq.setting.main.processor.AccountSecurityItemProcessor",
            "com.tencent.mobileqq.setting.main.processor.AboutItemProcessor"
    };

    private static final Set<List<?>> SEEN_LISTS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private static volatile boolean sInstalledAnyProvider;
    private static volatile boolean sInjectedAtLeastOnce;

    static void install(XposedModule module, ClassLoader loader) {
        if (loader == null) return;

        int hooked = 0;
        for (String providerName : PROVIDERS) {
            Class<?> provider;
            try {
                provider = Class.forName(providerName, false, loader);
            } catch (Throwable ignored) {
                continue;
            }

            for (Method method : provider.getDeclaredMethods()) {
                if (!List.class.isAssignableFrom(method.getReturnType())) continue;
                Class<?>[] p = method.getParameterTypes();
                if (p.length != 1 || !Context.class.isAssignableFrom(p[0])) continue;

                String key = provider.getName() + "#" + method.getName();
                try {
                    method.setAccessible(true);
                    module.hook(method)
                            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                            .intercept(chain -> {
                                Object original = chain.proceed();
                                if (!(original instanceof List)) return original;
                                Object arg0 = chain.getArg(0);
                                if (!(arg0 instanceof Context)) return original;

                                try {
                                    @SuppressWarnings("unchecked")
                                    List<Object> list = (List<Object>) original;
                                    return injectIntoResult(
                                            module,
                                            (Context) arg0,
                                            loader,
                                            provider.getName(),
                                            list
                                    );
                                } catch (Throwable t) {
                                    module.log(Log.ERROR, TAG,
                                            "Injection failed in " + provider.getName()
                                                    + "#" + method.getName(), t);
                                    return original;
                                }
                            });
                    hooked++;
                    module.log(Log.INFO, TAG, "Hooked provider method: " + key);
                } catch (Throwable t) {
                    module.log(Log.ERROR, TAG, "Failed to hook provider method: " + key, t);
                }
            }
        }

        sInstalledAnyProvider = hooked > 0;
        if (hooked == 0) {
            module.log(Log.WARN, TAG,
                    "No known QQ settings provider found. Rendered-view fallback will remain active.");
        } else {
            module.log(Log.INFO, TAG, "QQ settings provider hooks installed: " + hooked);
        }
    }

    private static Object injectIntoResult(
            XposedModule module,
            Context context,
            ClassLoader loader,
            String providerName,
            List<Object> source
    ) throws Exception {
        synchronized (SEEN_LISTS) {
            if (SEEN_LISTS.contains(source)) return source;
            SEEN_LISTS.add(source);
        }

        if (source.isEmpty()) {
            module.log(Log.WARN, TAG, "Settings provider returned an empty group list");
            return source;
        }

        ProcessorFactory processorFactory = findSimpleItemProcessor(module, loader);
        if (processorFactory == null) {
            module.log(Log.WARN, TAG,
                    "Could not locate QQ SimpleItemProcessor-compatible class");
            return source;
        }

        int iconId = findHostSettingsIcon(context);
        Object entry = processorFactory.newItem(
                context,
                ENTRY_ID,
                "ACE 工具箱",
                iconId
        );
        processorFactory.attachClick(entry, () -> openAce(context));

        ArrayList<Object> itemList = new ArrayList<>(1);
        itemList.add(entry);

        Object group = makeGroup(source.get(0).getClass(), itemList);
        if (group == null) {
            module.log(Log.WARN, TAG,
                    "Could not construct QQ settings group from "
                            + source.get(0).getClass().getName());
            return source;
        }

        int insertIndex = providerName.contains("NewSettingConfigProvider")
                || providerName.endsWith(".b") ? 2 : 1;
        insertIndex = Math.max(0, Math.min(insertIndex, source.size()));

        try {
            source.add(insertIndex, group);
            sInjectedAtLeastOnce = true;
            module.log(Log.INFO, TAG,
                    "ACE native settings entry injected; provider=" + providerName
                            + "; index=" + insertIndex
                            + "; processor=" + entry.getClass().getName());
            return source;
        } catch (UnsupportedOperationException immutable) {
            ArrayList<Object> copy = new ArrayList<>(source);
            copy.add(insertIndex, group);
            sInjectedAtLeastOnce = true;
            module.log(Log.INFO, TAG,
                    "ACE native settings entry injected via copied list; provider="
                            + providerName + "; index=" + insertIndex);
            return copy;
        }
    }

    private static ProcessorFactory findSimpleItemProcessor(
            XposedModule module,
            ClassLoader loader
    ) {
        Class<?> base = null;
        for (String hint : BASE_PROCESSOR_HINTS) {
            try {
                Class<?> cls = Class.forName(hint, false, loader);
                base = cls.getSuperclass();
                if (base != null) break;
            } catch (Throwable ignored) {}
        }
        if (base == null) {
            module.log(Log.WARN, TAG, "Unable to determine QQ setting item processor base class");
            return null;
        }

        ArrayList<String> names = new ArrayList<>();
        // Historical/current non-obfuscated names seen across QQ NT generations.
        for (char c = 'a'; c <= 'z'; c++) {
            names.add("com.tencent.mobileqq.setting.processor." + c);
            names.add("as3." + c);
        }

        ProcessorFactory best = null;
        int candidates = 0;
        for (String name : names) {
            Class<?> cls;
            try {
                cls = Class.forName(name, false, loader);
            } catch (Throwable ignored) {
                continue;
            }
            if (cls.getSuperclass() != base) continue;

            ProcessorFactory f = ProcessorFactory.tryCreate(cls);
            if (f != null) {
                candidates++;
                if (best == null || f.score > best.score) best = f;
            }
        }

        if (best != null) {
            module.log(Log.INFO, TAG,
                    "Simple item processor selected: " + best.type.getName()
                            + "; candidates=" + candidates);
        }
        return best;
    }

    private static Object makeGroup(Class<?> groupType, List<Object> items) {
        Constructor<?>[] constructors = groupType.getDeclaredConstructors();
        for (Constructor<?> c : constructors) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length < 3 || !List.class.isAssignableFrom(p[0])) continue;

            Object[] args = new Object[p.length];
            boolean supported = true;
            for (int i = 0; i < p.length; i++) {
                Class<?> t = p[i];
                if (i == 0) {
                    args[i] = items;
                } else if (t == String.class || CharSequence.class.isAssignableFrom(t)) {
                    args[i] = "";
                } else if (t == int.class || t == Integer.class) {
                    args[i] = 6;
                } else if (t == boolean.class || t == Boolean.class) {
                    args[i] = false;
                } else if (!t.isPrimitive()) {
                    args[i] = null;
                } else {
                    supported = false;
                    break;
                }
            }
            if (!supported) continue;

            try {
                c.setAccessible(true);
                return c.newInstance(args);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static int findHostSettingsIcon(Context context) {
        String pkg = context.getPackageName();
        String[] names = {"qui_tuning", "qui_setting", "qui_settings", "qui_menu_setting"};
        for (String name : names) {
            try {
                int id = context.getResources().getIdentifier(name, "drawable", pkg);
                if (id != 0) return id;
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    private static void openAce(Context context) {
        Activity activity = findActivity(context);
        if (activity != null) {
            activity.runOnUiThread(() ->
                    HostCleanerDialog.show(activity, HostPackages.QQ));
            return;
        }

        // Very defensive fallback: open the standalone ACE app if QQ supplied a non-Activity Context.
        try {
            Intent launch = context.getPackageManager()
                    .getLaunchIntentForPackage("com.ace.toolbox");
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launch);
            }
        } catch (Throwable ignored) {}
    }

    private static Activity findActivity(Context context) {
        Context current = context;
        for (int i = 0; i < 12 && current != null; i++) {
            if (current instanceof Activity) return (Activity) current;
            if (current instanceof ContextWrapper) {
                Context next = ((ContextWrapper) current).getBaseContext();
                if (next == current) break;
                current = next;
            } else {
                break;
            }
        }
        return null;
    }

    static boolean providerHookInstalled() {
        return sInstalledAnyProvider;
    }

    static boolean nativeEntryInjected() {
        return sInjectedAtLeastOnce;
    }

    private static final class ProcessorFactory {
        final Class<?> type;
        final Constructor<?> constructor;
        final Method clickSetter;
        final int score;

        private ProcessorFactory(
                Class<?> type,
                Constructor<?> constructor,
                Method clickSetter,
                int score
        ) {
            this.type = type;
            this.constructor = constructor;
            this.clickSetter = clickSetter;
            this.score = score;
        }

        static ProcessorFactory tryCreate(Class<?> type) {
            Constructor<?> bestCtor = null;
            int ctorScore = 0;

            for (Constructor<?> c : type.getDeclaredConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                if (p.length != 4 && p.length != 5) continue;
                if (!Context.class.isAssignableFrom(p[0])) continue;
                if (p[1] != int.class && p[1] != Integer.class) continue;
                if (!CharSequence.class.isAssignableFrom(p[2])
                        && p[2] != String.class) continue;
                if (p[3] != int.class && p[3] != Integer.class) continue;
                if (p.length == 5 && p[4] != String.class) continue;

                int score = p.length == 5 ? 15 : 10;
                if (score > ctorScore) {
                    ctorScore = score;
                    bestCtor = c;
                }
            }
            if (bestCtor == null) return null;

            Method bestSetter = null;
            int methodScore = 0;
            for (Method m : type.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getReturnType() != void.class) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length != 1 || !p[0].isInterface()) continue;

                int score = 1;
                for (Method im : p[0].getMethods()) {
                    if ("invoke".equals(im.getName())
                            && im.getParameterTypes().length == 0) {
                        score += 20;
                    }
                }
                String n = p[0].getName();
                if (n.contains("kotlin.jvm.functions.Function0")) score += 40;

                if (score > methodScore) {
                    methodScore = score;
                    bestSetter = m;
                }
            }
            if (bestSetter == null || methodScore < 20) return null;

            bestCtor.setAccessible(true);
            bestSetter.setAccessible(true);
            return new ProcessorFactory(
                    type,
                    bestCtor,
                    bestSetter,
                    ctorScore + methodScore
            );
        }

        Object newItem(
                Context context,
                int id,
                CharSequence title,
                int iconId
        ) throws Exception {
            Class<?>[] p = constructor.getParameterTypes();
            if (p.length == 5) {
                return constructor.newInstance(context, id, title, iconId, null);
            }
            return constructor.newInstance(context, id, title, iconId);
        }

        void attachClick(Object item, Runnable callback) throws Exception {
            Class<?> listenerType = clickSetter.getParameterTypes()[0];
            Object unit = null;
            try {
                Class<?> unitType = Class.forName(
                        "kotlin.Unit",
                        false,
                        listenerType.getClassLoader()
                );
                unit = unitType.getField("INSTANCE").get(null);
            } catch (Throwable ignored) {}
            final Object unitValue = unit;

            Object proxy = Proxy.newProxyInstance(
                    listenerType.getClassLoader(),
                    new Class[]{listenerType},
                    (p, m, args) -> {
                        String name = m.getName();
                        if ("invoke".equals(name)) {
                            callback.run();
                            return unitValue;
                        }
                        if ("toString".equals(name)) return "ACEQQSettingsClick";
                        if ("hashCode".equals(name)) return System.identityHashCode(p);
                        if ("equals".equals(name)) {
                            return args != null && args.length == 1 && p == args[0];
                        }
                        if (m.getReturnType() == boolean.class) return false;
                        if (m.getReturnType() == int.class) return 0;
                        if (m.getReturnType() == long.class) return 0L;
                        return null;
                    }
            );
            clickSetter.invoke(item, proxy);
        }
    }

    private QqSettingsProviderInjector() {}
}
