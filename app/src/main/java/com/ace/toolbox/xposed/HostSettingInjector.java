package com.ace.toolbox.xposed;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.WeakHashMap;

final class HostSettingInjector {
    private static final String TAG = "ACE-Inject";
    private static final Map<Activity, Boolean> DONE = new WeakHashMap<>();

    static void inject(Activity activity, String pkg) {
        if (!HostConfig.cleanEnabled(activity)) return;
        synchronized (DONE) {
            if (Boolean.TRUE.equals(DONE.get(activity))) return;
        }
        if (tryNativePreference(activity, pkg)) {
            synchronized (DONE) { DONE.put(activity, true); }
            return;
        }
        if (injectFallbackRow(activity, pkg)) {
            synchronized (DONE) { DONE.put(activity, true); }
        }
    }

    private static boolean tryNativePreference(Activity activity, String pkg) {
        try {
            Method getter = ReflectionUtils.findMethod(activity.getClass(), "getPreferenceScreen");
            if (getter == null) return false;
            Object screen = getter.invoke(activity);
            if (screen == null) return false;

            Method add = ReflectionUtils.findOneArgMethod(screen.getClass(), "addPreference");
            if (add == null) return false;
            Class<?> prefBase = add.getParameterTypes()[0];
            Class<?> prefClass = choosePreferenceClass(activity, prefBase);
            Object pref = ReflectionUtils.constructPreference(prefClass, activity);

            invokeIfPresent(pref, "setKey", "ace_toolkit_entry");
            invokeIfPresent(pref, "setTitle", "ACE 工具箱");
            invokeIfPresent(pref, "setSummary", HostPackages.QQ.equals(pkg)
                    ? "缓存清理 · SSAID · Hook 规则" : "缓存清理 · Hook 规则");

            Method listenerSetter = ReflectionUtils.findOneArgMethod(pref.getClass(), "setOnPreferenceClickListener");
            if (listenerSetter != null && listenerSetter.getParameterTypes()[0].isInterface()) {
                Class<?> listenerType = listenerSetter.getParameterTypes()[0];
                Object listener = Proxy.newProxyInstance(listenerType.getClassLoader(), new Class[]{listenerType}, (proxy, method, args) -> {
                    if (method.getName().toLowerCase().contains("click")) {
                        HostCleanerDialog.show(activity, pkg);
                        return true;
                    }
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                });
                listenerSetter.invoke(pref, listener);
            } else return false;

            add.setAccessible(true);
            Object result = add.invoke(screen, pref);
            Log.i(TAG, "Native preference injected: " + prefClass.getName());
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Throwable t) {
            Log.w(TAG, "Native preference path unavailable: " + t.getMessage());
            return false;
        }
    }

    private static Class<?> choosePreferenceClass(Activity activity, Class<?> addParam) throws Exception {
        if (!addParam.isInterface() && !java.lang.reflect.Modifier.isAbstract(addParam.getModifiers())) return addParam;
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
                try { m.invoke(target, value); } catch (Throwable ignored) {}
                return;
            }
        }
    }

    private static boolean injectFallbackRow(Activity activity, String pkg) {
        try {
            View content = activity.findViewById(android.R.id.content);
            if (!(content instanceof ViewGroup)) return false;
            ViewGroup root = (ViewGroup) content;
            if (root.findViewWithTag("ace_toolkit_fallback") != null) return true;

            LinearLayout row = new LinearLayout(activity);
            row.setTag("ace_toolkit_fallback");
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(activity, 20), dp(activity, 13), dp(activity, 20), dp(activity, 13));
            row.setClickable(true);
            row.setFocusable(true);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.argb(238, 255, 255, 255));
            bg.setCornerRadius(dp(activity, 18));
            row.setBackground(bg);

            TextView title = new TextView(activity);
            title.setText("ACE 工具箱");
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            title.setTextColor(Color.rgb(20, 20, 20));
            TextView sub = new TextView(activity);
            sub.setText("缓存清理 · 规则适配");
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            sub.setTextColor(Color.rgb(110, 110, 110));
            row.addView(title);
            row.addView(sub);
            row.setOnClickListener(v -> HostCleanerDialog.show(activity, pkg));

            ViewGroup target = findLargestContainer(root);
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(activity, 14), dp(activity, 8), dp(activity, 14), dp(activity, 8));
            target.addView(row, lp);
            Log.i(TAG, "Fallback setting row injected");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Fallback injection failed", t);
            return false;
        }
    }

    private static ViewGroup findLargestContainer(ViewGroup root) {
        ViewGroup best = root;
        int bestChildren = root.getChildCount();
        java.util.ArrayDeque<ViewGroup> q = new java.util.ArrayDeque<>();
        q.add(root);
        int scanned = 0;
        while (!q.isEmpty() && scanned++ < 300) {
            ViewGroup g = q.removeFirst();
            if (g.getChildCount() > bestChildren) { best = g; bestChildren = g.getChildCount(); }
            for (int i = 0; i < g.getChildCount(); i++) {
                View v = g.getChildAt(i);
                if (v instanceof ViewGroup) q.add((ViewGroup) v);
            }
        }
        return best;
    }

    private static int dp(Context c, int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    private HostSettingInjector() {}
}
