package com.ace.toolbox.xposed;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Injects the host-side cleanup entry.
 *
 * Compatibility mode is the default. In that mode ACE does not modify QQ/WeChat's internal
 * PreferenceScreen at all; it adds a small overlay entry to the Activity content layer instead.
 * This is intentionally less invasive and coexists better with modules such as FunBox that also
 * modify the same settings screen.
 */
final class HostSettingInjector {
    private static final String TAG = "ACE-Inject";
    private static final String SAFE_ENTRY_TAG = "ace_toolkit_safe_entry";
    private static final String NATIVE_KEY = "ace_toolkit_entry";

    static void scheduleMaybeInject(Activity activity, String pkg) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        View decor = activity.getWindow().getDecorView();
        if (decor == null) return;
        long[] delays = {80L, 350L, 900L};
        for (long delay : delays) {
            decor.postDelayed(() -> maybeInject(activity, pkg), delay);
        }
    }

    static void maybeInject(Activity activity, String pkg) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (!HostConfig.cleanEnabled(activity)) {
            removeSafeEntry(activity);
            return;
        }
        if (!looksLikeSettings(activity, pkg)) {
            removeSafeEntry(activity);
            return;
        }
        injectKnownSettings(activity, pkg);
    }

    static void injectKnownSettings(Activity activity, String pkg) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (!HostConfig.cleanEnabled(activity)) {
            removeSafeEntry(activity);
            return;
        }

        // Native host Preference injection is opt-in because two modules mutating the same
        // PreferenceScreen can leave QQ/WeChat's internal adapter in an inconsistent state.
        if (!HostConfig.compatibilityMode(activity) && tryNativePreference(activity, pkg)) {
            removeSafeEntry(activity);
            return;
        }

        injectSafeEntry(activity, pkg);
    }

    private static boolean tryNativePreference(Activity activity, String pkg) {
        try {
            Method getter = ReflectionUtils.findMethod(activity.getClass(), "getPreferenceScreen");
            if (getter == null) return false;
            getter.setAccessible(true);
            Object screen = getter.invoke(activity);
            if (screen == null) return false;

            if (nativeEntryAlreadyExists(screen)) return true;

            Method add = ReflectionUtils.findOneArgMethod(screen.getClass(), "addPreference");
            if (add == null) return false;
            Class<?> prefBase = add.getParameterTypes()[0];
            Class<?> prefClass = choosePreferenceClass(activity, prefBase);
            Object pref = ReflectionUtils.constructPreference(prefClass, activity);

            invokeIfPresent(pref, "setKey", NATIVE_KEY);
            invokeIfPresent(pref, "setTitle", "ACE 清理");
            invokeIfPresent(pref, "setSummary", HostPackages.QQ.equals(pkg)
                    ? "安全缓存清理 · SSAID · Hook 规则" : "安全缓存清理 · Hook 规则");

            Method listenerSetter = ReflectionUtils.findOneArgMethod(pref.getClass(), "setOnPreferenceClickListener");
            if (listenerSetter == null || !listenerSetter.getParameterTypes()[0].isInterface()) return false;
            Class<?> listenerType = listenerSetter.getParameterTypes()[0];
            Object listener = Proxy.newProxyInstance(
                    listenerType.getClassLoader(),
                    new Class[]{listenerType},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if (name.toLowerCase(Locale.ROOT).contains("click")) {
                            HostCleanerDialog.show(activity, pkg);
                            return true;
                        }
                        if ("toString".equals(name)) return "ACEPreferenceClickListener";
                        if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                        if ("equals".equals(name)) return args != null && args.length == 1 && proxy == args[0];
                        if (method.getReturnType() == boolean.class) return false;
                        return null;
                    }
            );
            listenerSetter.setAccessible(true);
            listenerSetter.invoke(pref, listener);

            add.setAccessible(true);
            Object result = add.invoke(screen, pref);
            boolean ok = !(result instanceof Boolean) || (Boolean) result;
            if (ok) Log.i(TAG, "Native preference injected: " + prefClass.getName());
            return ok;
        } catch (Throwable t) {
            Log.w(TAG, "Native preference path unavailable: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }

    private static boolean nativeEntryAlreadyExists(Object screen) {
        try {
            Method find = ReflectionUtils.findOneArgMethod(screen.getClass(), "findPreference");
            if (find == null) return false;
            find.setAccessible(true);
            return find.invoke(screen, NATIVE_KEY) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Class<?> choosePreferenceClass(Activity activity, Class<?> addParam) throws Exception {
        if (!addParam.isInterface() && !java.lang.reflect.Modifier.isAbstract(addParam.getModifiers())) {
            return addParam;
        }
        ClassLoader cl = activity.getClassLoader();
        String[] known = {
                "com.tencent.mm.ui.base.preference.Preference",
                "androidx.preference.Preference",
                "android.preference.Preference"
        };
        for (String n : known) {
            try {
                Class<?> c = Class.forName(n, false, cl);
                if (addParam.isAssignableFrom(c)) return c;
            } catch (Throwable ignored) {}
        }
        throw new ClassNotFoundException("No concrete preference for " + addParam);
    }

    private static void invokeIfPresent(Object target, String method, Object value) {
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(method) && m.getParameterTypes().length == 1) {
                try {
                    m.setAccessible(true);
                    m.invoke(target, value);
                } catch (Throwable ignored) {}
                return;
            }
        }
    }

    /**
     * Compatibility entry. Uses Activity.addContentView rather than touching the host preference
     * adapter/model, which keeps this module isolated from other settings-injection modules.
     */
    private static boolean injectSafeEntry(Activity activity, String pkg) {
        try {
            View decor = activity.getWindow().getDecorView();
            if (decor == null) return false;
            View existing = decor.findViewWithTag(SAFE_ENTRY_TAG);
            if (existing != null) return true;

            TextView entry = new TextView(activity);
            entry.setTag(SAFE_ENTRY_TAG);
            entry.setText("ACE 清理");
            entry.setContentDescription("打开 ACE 安全清理");
            entry.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            entry.setTextColor(Color.WHITE);
            entry.setGravity(Gravity.CENTER);
            entry.setPadding(dp(activity, 16), dp(activity, 10), dp(activity, 16), dp(activity, 10));
            entry.setClickable(true);
            entry.setFocusable(true);
            entry.setElevation(dp(activity, 8));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.rgb(24, 119, 242));
            bg.setCornerRadius(dp(activity, 24));
            entry.setBackground(bg);
            entry.setOnClickListener(v -> HostCleanerDialog.show(activity, pkg));

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.END | Gravity.BOTTOM
            );
            lp.setMargins(dp(activity, 16), dp(activity, 16), dp(activity, 18), dp(activity, 78));
            activity.addContentView(entry, lp);
            Log.i(TAG, "Compatibility cleanup entry injected into " + activity.getClass().getName());
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Compatibility entry injection failed", t);
            return false;
        }
    }

    private static void removeSafeEntry(Activity activity) {
        try {
            View decor = activity.getWindow().getDecorView();
            if (decor == null) return;
            View entry = decor.findViewWithTag(SAFE_ENTRY_TAG);
            if (entry != null && entry.getParent() instanceof ViewGroup) {
                ((ViewGroup) entry.getParent()).removeView(entry);
            }
        } catch (Throwable ignored) {}
    }

    private static boolean looksLikeSettings(Activity activity, String pkg) {
        String className = activity.getClass().getName();
        if (HostPackages.QQ.equals(pkg)
                && "com.tencent.mobileqq.activity.QPublicFragmentActivity".equals(className)) {
            return true;
        }
        if (HostPackages.WECHAT.equals(pkg)
                && "com.tencent.mm.plugin.setting.ui.setting_new.MainSettingsUI".equals(className)) {
            return true;
        }

        String lowerClass = className.toLowerCase(Locale.ROOT);
        boolean classHint = lowerClass.contains("setting") || lowerClass.contains("settings");
        View decor = activity.getWindow().getDecorView();
        if (decor == null) return classHint;

        boolean hasSettingsTitle = false;
        boolean hasSettingsItem = false;
        ArrayDeque<View> queue = new ArrayDeque<>();
        Set<View> seen = new HashSet<>();
        queue.add(decor);
        int scanned = 0;
        while (!queue.isEmpty() && scanned++ < 900) {
            View v = queue.removeFirst();
            if (v == null || !seen.add(v)) continue;
            if (v instanceof TextView) {
                CharSequence cs = ((TextView) v).getText();
                if (cs != null) {
                    String s = cs.toString().trim().toLowerCase(Locale.ROOT);
                    if (s.equals("设置") || s.equals("settings") || s.equals("setting")) {
                        hasSettingsTitle = true;
                    }
                    if (HostPackages.QQ.equals(pkg)) {
                        if (s.contains("通用") || s.contains("隐私") || s.contains("账号")
                                || s.contains("辅助") || s.contains("关于qq") || s.contains("about qq")) {
                            hasSettingsItem = true;
                        }
                    } else {
                        if (s.contains("通用") || s.contains("隐私") || s.contains("账号与安全")
                                || s.contains("关于微信") || s.contains("about wechat")) {
                            hasSettingsItem = true;
                        }
                    }
                }
            }
            if (hasSettingsTitle && hasSettingsItem) return true;
            if (v instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) v;
                for (int i = 0; i < group.getChildCount(); i++) queue.addLast(group.getChildAt(i));
            }
        }
        return classHint && hasSettingsItem;
    }

    private static int dp(Context c, int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                c.getResources().getDisplayMetrics()
        );
    }

    private HostSettingInjector() {}
}
