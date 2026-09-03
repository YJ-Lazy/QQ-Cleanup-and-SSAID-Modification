package com.ace.toolbox.xposed;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.github.libxposed.api.XposedModule;

/**
 * Injects ACE as a real-looking settings-menu row.
 *
 * v0.1.9 compatibility strategy:
 * 1) Do NOT assume QPublicFragmentActivity itself means "QQ Settings". QQ NT reuses that
 *    container for many pages, including the main page. We require a settings text fingerprint.
 * 2) In compatibility mode we do NOT mutate QQ/WeChat's Preference model. Instead we locate
 *    the already-rendered vertical settings container and insert one ordinary Android View row
 *    next to existing setting rows. This visually behaves like a menu item while avoiding
 *    addPreference() races with FunBox and similar modules.
 * 3) Native Preference injection remains opt-in when compatibility mode is disabled.
 */
final class HostSettingInjector {
    private static final String TAG = "ACE-Inject";
    private static final String MENU_ENTRY_TAG = "ace_toolkit_menu_entry";
    private static final String NATIVE_KEY = "ace_toolkit_entry";
    private static volatile XposedModule LOGGER;

    static void setLogger(XposedModule module) { LOGGER = module; }

    private static void xlog(int level, String message) {
        XposedModule m = LOGGER;
        if (m != null) m.log(level, TAG, message);
    }

    static void scheduleMaybeInject(Activity activity, String pkg) {
        if (!alive(activity)) return;
        View decor = activity.getWindow().getDecorView();
        if (decor == null) return;
        long[] delays = {120L, 420L, 950L, 1800L};
        for (long delay : delays) {
            decor.postDelayed(() -> maybeInject(activity, pkg), delay);
        }
    }

    static void maybeInject(Activity activity, String pkg) {
        if (!alive(activity)) return;
        boolean settingsPage = looksLikeSettings(activity, pkg);
        xlog(Log.INFO, "Page probe: " + activity.getClass().getName()
                + "; settings=" + settingsPage
                + "; cleanEnabled=" + HostConfig.cleanEnabled(activity));
        if (!HostConfig.cleanEnabled(activity) || !settingsPage) {
            removeMenuEntry(activity);
            return;
        }
        injectKnownSettings(activity, pkg);
    }

    static void injectKnownSettings(Activity activity, String pkg) {
        if (!alive(activity)) return;
        if (!HostConfig.cleanEnabled(activity)) {
            removeMenuEntry(activity);
            return;
        }

        // WeChat settings are adapter-backed MMPreference screens on current builds.
        // A rendered LinearLayout row is therefore unreliable/impossible. Prefer the native
        // Preference model for WeChat even when compatibility mode is enabled.
        if (HostPackages.WECHAT.equals(pkg)) {
            if (tryNativePreference(activity, pkg)) {
                removeMenuEntry(activity);
                xlog(Log.INFO, "WeChat native Preference entry injected into "
                        + activity.getClass().getName());
                return;
            }

            // Current WeChat MMPreference pages are commonly backed by ListView.
            // If the internal Preference API is obfuscated/changed, add ACE as a real ListView
            // footer row instead of trying to insert into an adapter-managed child container.
            if (injectWechatListFooter(activity, pkg)) {
                xlog(Log.INFO, "WeChat ListView footer entry injected into "
                        + activity.getClass().getName());
                return;
            }

            // Newer WeChat uses com.tencent.mm.view.recyclerview.WxRecyclerView in MainSettingsUI.
            // Try to place ACE next to that RecyclerView in the surrounding scroll/layout hierarchy.
            if (injectWechatRecyclerEntry(activity, pkg)) {
                xlog(Log.INFO, "WeChat RecyclerView settings entry injected into "
                        + activity.getClass().getName());
                return;
            }

            xlog(Log.WARN,
                    "WeChat native Preference/ListView/RecyclerView injection unavailable; "
                            + "trying generic rendered fallback");
            dumpWechatHierarchyOnce(activity);
        } else if (!HostConfig.compatibilityMode(activity) && tryNativePreference(activity, pkg)) {
            removeMenuEntry(activity);
            return;
        }

        if (!injectMenuRow(activity, pkg)) {
            Log.w(TAG, "No safe rendered settings container found; ACE menu row not inserted");
            xlog(Log.WARN, "Settings page detected, but no safe rendered menu container was found");
        }
    }


private static boolean injectWechatListFooter(Activity activity, String pkg) {
    try {
        View decor = activity.getWindow().getDecorView();
        if (decor == null) return false;

        ListView list = findFirstListView(decor);
        if (list == null) {
            xlog(Log.WARN, "WeChat settings detected, but no ListView was found");
            return false;
        }

        // Search existing fixed views / visible hierarchy for our row first.
        if (decor.findViewWithTag(MENU_ENTRY_TAG) != null) return true;

        LinearLayout row = buildMenuRow(activity, pkg, null);
        row.setMinimumHeight(dp(activity, 62));

        LinearLayout wrapper = new LinearLayout(activity);
        wrapper.setTag(MENU_ENTRY_TAG);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(0, dp(activity, 6), 0, dp(activity, 6));
        wrapper.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // addFooterView is exactly what we need for MMPreference's adapter-backed ListView.
        // It avoids mutating WeChat's internal preference data classes.
        list.addFooterView(wrapper, null, true);

        Log.i(TAG, "WeChat ListView footer injected: " + list.getClass().getName());
        xlog(Log.INFO, "WeChat ListView footer injected: " + list.getClass().getName()
                + "; adapter=" + (list.getAdapter() == null
                ? "null" : list.getAdapter().getClass().getName()));
        return true;
    } catch (Throwable t) {
        Log.e(TAG, "WeChat ListView footer injection failed", t);
        xlog(Log.ERROR, "WeChat ListView footer injection failed: "
                + t.getClass().getSimpleName() + ": " + t.getMessage());
        return false;
    }
}

private static ListView findFirstListView(View root) {
    if (root instanceof ListView) return (ListView) root;
    if (!(root instanceof ViewGroup)) return null;

    ViewGroup group = (ViewGroup) root;
    for (int i = 0; i < group.getChildCount(); i++) {
        ListView found = findFirstListView(group.getChildAt(i));
        if (found != null) return found;
    }
    return null;
}


private static final Set<String> WECHAT_HIERARCHY_DUMPED =
        Collections.synchronizedSet(new HashSet<>());

private static void dumpWechatHierarchyOnce(Activity activity) {
    String key = activity.getClass().getName();
    if (!WECHAT_HIERARCHY_DUMPED.add(key)) return;

    try {
        View decor = activity.getWindow().getDecorView();
        if (decor == null) return;

        ArrayDeque<View> queue = new ArrayDeque<>();
        queue.add(decor);
        int shown = 0;

        while (!queue.isEmpty() && shown < 80) {
            View v = queue.removeFirst();
            StringBuilder line = new StringBuilder()
                    .append("WeChat view[").append(shown).append("] ")
                    .append(v.getClass().getName())
                    .append("; id=").append(v.getId())
                    .append("; visible=").append(v.getVisibility() == View.VISIBLE);

            if (v instanceof TextView) {
                String text = ((TextView) v).getText() == null
                        ? "" : ((TextView) v).getText().toString().trim();
                if (!text.isEmpty()) {
                    line.append("; text=").append(text.length() > 40
                            ? text.substring(0, 40) : text);
                }
            }

            xlog(Log.INFO, line.toString());
            shown++;

            if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) {
                    View child = g.getChildAt(i);
                    if (child != null) queue.addLast(child);
                }
            }
        }
    } catch (Throwable t) {
        xlog(Log.WARN, "WeChat hierarchy dump failed: "
                + t.getClass().getSimpleName() + ": " + t.getMessage());
    }
}


private static boolean injectWechatRecyclerEntry(Activity activity, String pkg) {
    try {
        View decor = activity.getWindow().getDecorView();
        if (decor == null) return false;
        if (decor.findViewWithTag(MENU_ENTRY_TAG) != null) return true;

        View recycler = findFirstRecyclerViewLike(decor);
        if (recycler == null) {
            xlog(Log.WARN, "WeChat settings detected, but no RecyclerView-like view was found");
            return false;
        }

        xlog(Log.INFO, "WeChat RecyclerView found: " + recycler.getClass().getName()
                + "; id=" + recycler.getId());
        logParentChain(recycler);

        LinearLayout row = buildMenuRow(activity, pkg, null);
        row.setMinimumHeight(dp(activity, 62));

        // Preferred path: find a vertical LinearLayout ancestor. Insert ACE immediately after
        // the branch that contains the RecyclerView so it participates in the surrounding
        // scroll layout rather than fighting RecyclerView's adapter children.
        View branch = recycler;
        ViewGroup parent = recycler.getParent() instanceof ViewGroup
                ? (ViewGroup) recycler.getParent() : null;

        while (parent != null && parent != decor) {
            if (parent instanceof LinearLayout
                    && ((LinearLayout) parent).getOrientation() == LinearLayout.VERTICAL) {
                LinearLayout vertical = (LinearLayout) parent;
                int branchIndex = vertical.indexOfChild(branch);
                if (branchIndex >= 0) {
                    LinearLayout wrapper = buildWechatEntryWrapper(activity, row);
                    vertical.addView(
                            wrapper,
                            Math.min(branchIndex + 1, vertical.getChildCount()),
                            new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT)
                    );
                    xlog(Log.INFO, "WeChat RecyclerView entry inserted after branch in "
                            + vertical.getClass().getName()
                            + "; index=" + (branchIndex + 1));
                    return true;
                }
            }

            branch = parent;
            parent = parent.getParent() instanceof ViewGroup
                    ? (ViewGroup) parent.getParent() : null;
        }

        // Fallback: place a bottom ACE row in the nearest FrameLayout ancestor and reserve
        // bottom space on the RecyclerView so the last WeChat item is not covered.
        branch = recycler;
        parent = recycler.getParent() instanceof ViewGroup
                ? (ViewGroup) recycler.getParent() : null;
        while (parent != null && parent != decor) {
            if (parent instanceof FrameLayout) {
                FrameLayout frame = (FrameLayout) parent;
                LinearLayout wrapper = buildWechatEntryWrapper(activity, row);
                wrapper.setElevation(dp(activity, 4));

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM
                );
                lp.leftMargin = dp(activity, 12);
                lp.rightMargin = dp(activity, 12);
                lp.bottomMargin = dp(activity, 8);
                frame.addView(wrapper, lp);

                if (recycler instanceof ViewGroup) {

                    ((ViewGroup) recycler).setClipToPadding(false);

                }
                recycler.setPadding(
                        recycler.getPaddingLeft(),
                        recycler.getPaddingTop(),
                        recycler.getPaddingRight(),
                        recycler.getPaddingBottom() + dp(activity, 78)
                );

                xlog(Log.INFO, "WeChat RecyclerView overlay entry added to "
                        + frame.getClass().getName());
                return true;
            }
            branch = parent;
            parent = parent.getParent() instanceof ViewGroup
                    ? (ViewGroup) parent.getParent() : null;
        }

        return false;
    } catch (Throwable t) {
        Log.e(TAG, "WeChat RecyclerView injection failed", t);
        xlog(Log.ERROR, "WeChat RecyclerView injection failed: "
                + t.getClass().getSimpleName() + ": " + t.getMessage());
        return false;
    }
}

private static LinearLayout buildWechatEntryWrapper(Activity activity, View row) {
    LinearLayout wrapper = new LinearLayout(activity);
    wrapper.setTag(MENU_ENTRY_TAG);
    wrapper.setOrientation(LinearLayout.VERTICAL);
    wrapper.setPadding(0, dp(activity, 6), 0, dp(activity, 6));
    wrapper.setBackgroundColor(Color.TRANSPARENT);
    wrapper.addView(row, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));
    return wrapper;
}

private static View findFirstRecyclerViewLike(View root) {
    if (root == null) return null;
    String name = root.getClass().getName();
    if (name.contains("RecyclerView")) return root;
    if (!(root instanceof ViewGroup)) return null;

    ViewGroup group = (ViewGroup) root;
    for (int i = 0; i < group.getChildCount(); i++) {
        View found = findFirstRecyclerViewLike(group.getChildAt(i));
        if (found != null) return found;
    }
    return null;
}

private static void logParentChain(View view) {
    try {
        View current = view;
        for (int depth = 0; depth < 10; depth++) {
            Object p = current.getParent();
            if (!(p instanceof View)) break;
            current = (View) p;
            xlog(Log.INFO, "WeChat recycler parent[" + depth + "] "
                    + current.getClass().getName()
                    + "; id=" + current.getId());
        }
    } catch (Throwable ignored) {}
}

    private static boolean injectMenuRow(Activity activity, String pkg) {
        try {
            View decor = activity.getWindow().getDecorView();
            if (decor == null) return false;
            if (decor.findViewWithTag(MENU_ENTRY_TAG) != null) return true;

            MenuTarget target = findMenuTarget(decor, pkg);
            if (target == null || target.parent == null) return false;
            if (isAdapterManaged(target.parent)) return false;

            LinearLayout row = buildMenuRow(activity, pkg, target.anchorRow);
            int insertAt = Math.max(0, Math.min(target.index, target.parent.getChildCount()));
            target.parent.addView(row, insertAt);

            Log.i(TAG, "Rendered settings menu row inserted into "
                    + target.parent.getClass().getName() + " at " + insertAt);
            xlog(Log.INFO, "Rendered fallback menu row inserted into "
                    + target.parent.getClass().getName() + " at " + insertAt);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Rendered menu-row injection failed", t);
            return false;
        }
    }

    private static LinearLayout buildMenuRow(Activity activity, String pkg, View anchorRow) {
        LinearLayout row = new LinearLayout(activity);
        row.setTag(MENU_ENTRY_TAG);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(activity, 58));
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription("ACE 工具箱，打开安全清理");

        int horizontal = dp(activity, 18);
        int vertical = dp(activity, 11);
        if (anchorRow != null) {
            horizontal = Math.max(dp(activity, 14), anchorRow.getPaddingLeft());
            vertical = Math.max(dp(activity, 8), Math.min(dp(activity, 16), anchorRow.getPaddingTop()));
        }
        row.setPadding(horizontal, vertical, horizontal, vertical);

        Drawable copied = cloneBackground(anchorRow);
        if (copied != null) {
            row.setBackground(copied);
        } else {
            TypedValue selectable = new TypedValue();
            if (activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, selectable, true)
                    && selectable.resourceId != 0) {
                row.setBackgroundResource(selectable.resourceId);
            }
        }

        TextView icon = new TextView(activity);
        icon.setText("A");
        icon.setTextColor(Color.WHITE);
        icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        icon.setGravity(Gravity.CENTER);
        icon.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(Color.rgb(24, 119, 242));
        icon.setBackground(iconBg);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(activity, 36), dp(activity, 36));
        iconLp.setMarginEnd(dp(activity, 14));
        row.addView(icon, iconLp);

        LinearLayout texts = new LinearLayout(activity);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(activity);
        title.setText("ACE 工具箱");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTextColor(resolveTextColor(activity, android.R.attr.textColorPrimary, Color.rgb(30, 30, 30)));
        title.setSingleLine(true);

        TextView summary = new TextView(activity);
        summary.setText(HostPackages.QQ.equals(pkg)
                ? "安全清理 · SSAID"
                : "安全清理");
        summary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        summary.setTextColor(resolveTextColor(activity, android.R.attr.textColorSecondary, Color.rgb(120, 120, 120)));
        summary.setSingleLine(true);

        texts.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        texts.addView(summary, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(texts, textLp);

        TextView arrow = new TextView(activity);
        arrow.setText("›");
        arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 27);
        arrow.setTextColor(resolveTextColor(activity, android.R.attr.textColorSecondary, Color.rgb(150, 150, 150)));
        arrow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams arrowLp = new LinearLayout.LayoutParams(
                dp(activity, 26), ViewGroup.LayoutParams.MATCH_PARENT);
        row.addView(arrow, arrowLp);

        row.setOnClickListener(v -> HostCleanerDialog.show(activity, pkg));
        return row;
    }

    private static MenuTarget findMenuTarget(View root, String pkg) {
        List<TextHit> hits = new ArrayList<>();
        ArrayDeque<View> queue = new ArrayDeque<>();
        Set<View> seen = new HashSet<>();
        queue.add(root);
        int scanned = 0;

        while (!queue.isEmpty() && scanned++ < 1600) {
            View v = queue.removeFirst();
            if (v == null || !seen.add(v)) continue;
            if (v instanceof TextView) {
                CharSequence cs = ((TextView) v).getText();
                if (cs != null) {
                    String s = normalize(cs.toString());
                    int priority = anchorPriority(s, pkg);
                    if (priority > 0) hits.add(new TextHit(v, s, priority));
                }
            }
            if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) queue.addLast(g.getChildAt(i));
            }
        }

        MenuTarget best = null;
        int bestScore = Integer.MIN_VALUE;

        for (TextHit hit : hits) {
            View child = hit.view;
            ViewParentWalk walk = walkToVerticalContainer(child);
            if (walk == null) continue;

            int siblingSettingCount = countSettingRows(walk.parent, pkg);
            if (siblingSettingCount < 2) continue;

            int index = walk.parent.indexOfChild(walk.directChild);
            if (index < 0) continue;

            // Prefer inserting before "About", otherwise immediately after a strong normal setting.
            int targetIndex = hit.priority >= 90 ? index : index + 1;
            int score = hit.priority + siblingSettingCount * 8;
            if (walk.parent instanceof LinearLayout
                    && ((LinearLayout) walk.parent).getOrientation() == LinearLayout.VERTICAL) score += 30;
            if (walk.directChild.getHeight() >= dp(walk.directChild.getContext(), 40)) score += 8;
            if (isAdapterManaged(walk.parent)) score -= 1000;

            if (score > bestScore) {
                bestScore = score;
                best = new MenuTarget(walk.parent, walk.directChild, targetIndex);
            }
        }
        return best;
    }

    /**
     * Ascend from a setting label until we find the vertical LinearLayout that owns multiple
     * rendered setting rows. Restricting to LinearLayout intentionally avoids touching RecyclerView
     * or ListView internals managed by an adapter.
     */
    private static ViewParentWalk walkToVerticalContainer(View start) {
        View current = start;
        for (int depth = 0; depth < 8; depth++) {
            if (!(current.getParent() instanceof ViewGroup)) return null;
            ViewGroup parent = (ViewGroup) current.getParent();

            if (parent instanceof LinearLayout
                    && ((LinearLayout) parent).getOrientation() == LinearLayout.VERTICAL
                    && parent.getChildCount() >= 2
                    && !isAdapterManaged(parent)) {
                return new ViewParentWalk(parent, current);
            }
            current = parent;
        }
        return null;
    }

    private static int countSettingRows(ViewGroup parent, String pkg) {
        int count = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (subtreeContainsSettingText(parent.getChildAt(i), pkg)) count++;
        }
        return count;
    }

    private static boolean subtreeContainsSettingText(View root, String pkg) {
        ArrayDeque<View> q = new ArrayDeque<>();
        q.add(root);
        int scanned = 0;
        while (!q.isEmpty() && scanned++ < 120) {
            View v = q.removeFirst();
            if (v instanceof TextView) {
                CharSequence cs = ((TextView) v).getText();
                if (cs != null && anchorPriority(normalize(cs.toString()), pkg) > 0) return true;
            }
            if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) q.addLast(g.getChildAt(i));
            }
        }
        return false;
    }

    private static int anchorPriority(String s, String pkg) {
        if (HostPackages.QQ.equals(pkg)) {
            if (containsAny(s, "关于qq与帮助", "关于qq", "aboutqq")) return 100;
            if (containsAny(s, "辅助功能", "通用", "隐私")) return 70;
            if (containsAny(s, "账号管理", "消息通知", "手机号码", "账号安全")) return 55;
        } else {
            if (containsAny(s, "关于微信", "aboutwechat", "帮助与反馈")) return 100;
            if (containsAny(s,
                    "通用",
                    "账号与安全",
                    "新消息通知",
                    "聊天",
                    "朋友权限",
                    "个人信息与权限",
                    "辅助功能",
                    "青少年模式",
                    "关怀模式")) return 70;
            if (containsAny(s,
                    "个人信息收集清单",
                    "第三方信息共享清单",
                    "插件")) return 55;
        }
        return 0;
    }

    private static boolean looksLikeSettings(Activity activity, String pkg) {
        View decor = activity.getWindow().getDecorView();
        if (decor == null) return false;

        boolean title = false;
        int settingHits = 0;
        Set<String> uniqueHits = new HashSet<>();

        ArrayDeque<View> queue = new ArrayDeque<>();
        Set<View> seen = new HashSet<>();
        queue.add(decor);
        int scanned = 0;

        while (!queue.isEmpty() && scanned++ < 1400) {
            View v = queue.removeFirst();
            if (v == null || !seen.add(v)) continue;
            if (v instanceof TextView) {
                CharSequence cs = ((TextView) v).getText();
                if (cs != null) {
                    String raw = cs.toString().trim();
                    String s = normalize(raw);
                    if ("设置".equals(raw) || "settings".equals(s) || "setting".equals(s)) title = true;
                    int priority = anchorPriority(s, pkg);
                    if (priority > 0 && uniqueHits.add(s)) settingHits++;
                }
            }
            if (v instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) v;
                for (int i = 0; i < group.getChildCount(); i++) queue.addLast(group.getChildAt(i));
            }
        }

        String className = activity.getClass().getName().toLowerCase(Locale.ROOT);
        boolean classHint = className.contains("setting") || className.contains("settings");

        // QQ's QPublicFragmentActivity is a generic container, so class name alone is never enough.
        if (HostPackages.QQ.equals(pkg)) {
            // QPublicFragmentActivity is generic. Require multiple genuine settings rows.
            // Some QQ versions draw the large "设置" title outside a normal TextView, so allow
            // three strong row hits even when the title itself cannot be read.
            return (title && settingHits >= 2) || settingHits >= 3;
        }

        // Current WeChat logs prove MainSettingsUI is a dedicated settings Activity and that
        // LauncherUI can temporarily contain enough settings-like text to trigger a false positive.
        // Therefore WeChat injection is restricted to dedicated setting Activities only.
        String fullClassName = activity.getClass().getName();
        return fullClassName.equals(
                    "com.tencent.mm.plugin.setting.ui.setting_new.MainSettingsUI")
                || fullClassName.equals(
                    "com.tencent.mm.plugin.setting.ui.setting.SettingsUI")
                || (fullClassName.startsWith("com.tencent.mm.plugin.setting.ui.")
                    && fullClassName.endsWith("SettingsUI"));
    }


private static boolean tryNativePreference(Activity activity, String pkg) {
    try {
        Object screen = findPreferenceScreenObject(activity);
        if (screen == null) {
            xlog(Log.WARN, "Native preference: no PreferenceScreen-like object found in "
                    + activity.getClass().getName());
            return false;
        }

        xlog(Log.INFO, "Native preference: screen object=" + screen.getClass().getName());

        if (nativeEntryAlreadyExists(screen)) return true;

        Method add = ReflectionUtils.findOneArgMethod(screen.getClass(), "addPreference");
        if (add == null) {
            xlog(Log.WARN, "Native preference: screen has no addPreference method: "
                    + screen.getClass().getName());
            return false;
        }

        Class<?> prefBase = add.getParameterTypes()[0];
        Class<?> prefClass = choosePreferenceClass(activity, prefBase);
        Object pref = ReflectionUtils.constructPreference(prefClass, activity);

        invokeIfPresent(pref, "setKey", NATIVE_KEY);
        invokeIfPresent(pref, "setTitle", "ACE 工具箱");
        invokeIfPresent(pref, "setSummary", HostPackages.QQ.equals(pkg)
                ? "安全清理 · SSAID" : "安全清理");
        invokeIfPresent(pref, "setOrder", 0x5ACE);

        Method listenerSetter = ReflectionUtils.findOneArgMethod(
                pref.getClass(), "setOnPreferenceClickListener");
        if (listenerSetter == null || !listenerSetter.getParameterTypes()[0].isInterface()) {
            xlog(Log.WARN, "Native preference: click-listener setter unavailable on "
                    + pref.getClass().getName());
            return false;
        }

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
                    if ("equals".equals(name)) {
                        return args != null && args.length == 1 && proxy == args[0];
                    }
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                }
        );

        listenerSetter.setAccessible(true);
        listenerSetter.invoke(pref, listener);

        add.setAccessible(true);
        Object result = add.invoke(screen, pref);
        boolean ok = !(result instanceof Boolean) || (Boolean) result;
        if (ok) {
            invokeNoArgIfPresent(screen, "notifyDataSetChanged");
            invokeNoArgIfPresent(screen, "notifyChanged");
            invokeNoArgIfPresent(screen, "notifyDataSetInvalidated");
            invokeNoArgIfPresent(activity, "notifyDataSetChanged");

            Log.i(TAG, "Native preference injected: " + prefClass.getName()
                    + " into " + activity.getClass().getName());
            xlog(Log.INFO, "Native preference injected: " + prefClass.getName()
                    + " into " + activity.getClass().getName());
        }
        return ok;
    } catch (Throwable t) {
        Log.w(TAG, "Native preference path unavailable: "
                + t.getClass().getSimpleName() + ": " + t.getMessage());
        xlog(Log.WARN, "Native preference exception: "
                + t.getClass().getSimpleName() + ": " + t.getMessage());
        return false;
    }
}

/**
 * WeChat has changed/obfuscated its preference-screen getter across releases.
 * Instead of relying only on getPreferenceScreen(), discover any live object owned by the
 * Activity that exposes addPreference(oneArg). This works for MMPreference-derived screens
 * even when the accessor name is renamed.
 */
private static Object findPreferenceScreenObject(Activity activity) {
    // 1) Stable getter when present.
    try {
        Method getter = ReflectionUtils.findMethod(activity.getClass(), "getPreferenceScreen");
        if (getter != null && getter.getParameterTypes().length == 0) {
            getter.setAccessible(true);
            Object screen = getter.invoke(activity);
            if (isPreferenceScreenLike(screen)) return screen;
        }
    } catch (Throwable ignored) {}

    // 2) Probe zero-argument methods on the Activity hierarchy.
    Class<?> c = activity.getClass();
    while (c != null && c != Object.class) {
        for (Method m : c.getDeclaredMethods()) {
            if (m.getParameterTypes().length != 0) continue;
            Class<?> rt = m.getReturnType();
            if (rt == void.class || rt.isPrimitive() || rt == String.class) continue;

            String lower = m.getName().toLowerCase(Locale.ROOT);
            if (!(lower.contains("preference")
                    || lower.contains("screen")
                    || lower.length() <= 2)) {
                continue;
            }

            try {
                m.setAccessible(true);
                Object candidate = m.invoke(activity);
                if (isPreferenceScreenLike(candidate)) {
                    xlog(Log.INFO, "Native preference screen discovered via method "
                            + c.getName() + "#" + m.getName());
                    return candidate;
                }
            } catch (Throwable ignored) {}
        }
        c = c.getSuperclass();
    }

    // 3) Probe Activity fields. This is the most useful fallback for obfuscated WeChat builds.
    c = activity.getClass();
    while (c != null && c != Object.class) {
        for (Field field : c.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            try {
                field.setAccessible(true);
                Object candidate = field.get(activity);
                if (isPreferenceScreenLike(candidate)) {
                    xlog(Log.INFO, "Native preference screen discovered via field "
                            + c.getName() + "#" + field.getName()
                            + " -> " + candidate.getClass().getName());
                    return candidate;
                }
            } catch (Throwable ignored) {}
        }
        c = c.getSuperclass();
    }

    return null;
}

private static boolean isPreferenceScreenLike(Object candidate) {
    if (candidate == null) return false;
    return ReflectionUtils.findOneArgMethod(
            candidate.getClass(), "addPreference") != null;
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
                "com.tencent.mm.ui.base.preference.IconPreference",
                "com.tencent.mm.ui.base.preference.CheckBoxPreference",
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

    private static void invokeNoArgIfPresent(Object target, String method) {
        if (target == null) return;
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(method) && m.getParameterTypes().length == 0) {
                try {
                    m.setAccessible(true);
                    m.invoke(target);
                } catch (Throwable ignored) {}
                return;
            }
        }
    }

    private static void removeMenuEntry(Activity activity) {
        try {
            View decor = activity.getWindow().getDecorView();
            if (decor == null) return;
            View entry = decor.findViewWithTag(MENU_ENTRY_TAG);
            if (entry != null && entry.getParent() instanceof ViewGroup) {
                ((ViewGroup) entry.getParent()).removeView(entry);
            }
        } catch (Throwable ignored) {}
    }

    private static boolean isAdapterManaged(ViewGroup group) {
        String n = group.getClass().getName().toLowerCase(Locale.ROOT);
        return n.contains("recyclerview")
                || n.contains("listview")
                || n.contains("adapterview")
                || n.contains("viewpager");
    }

    private static Drawable cloneBackground(View v) {
        if (v == null || v.getBackground() == null) return null;
        try {
            Drawable.ConstantState state = v.getBackground().getConstantState();
            return state == null ? null : state.newDrawable().mutate();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int resolveTextColor(Context context, int attr, int fallback) {
        try {
            TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(attr, tv, true)) {
                if (tv.resourceId != 0) {
                    ColorStateList list = context.getResources().getColorStateList(tv.resourceId, context.getTheme());
                    return list.getDefaultColor();
                }
                if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    return tv.data;
                }
            }
        } catch (Throwable ignored) {}
        return fallback;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim()
                .toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("\u3000", "");
    }

    private static boolean containsAny(String s, String... needles) {
        for (String n : needles) if (s.contains(n)) return true;
        return false;
    }

    private static boolean alive(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    private static int dp(Context c, int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                v,
                c.getResources().getDisplayMetrics()
        );
    }

    private static final class TextHit {
        final View view;
        final String text;
        final int priority;
        TextHit(View view, String text, int priority) {
            this.view = view;
            this.text = text;
            this.priority = priority;
        }
    }

    private static final class ViewParentWalk {
        final ViewGroup parent;
        final View directChild;
        ViewParentWalk(ViewGroup parent, View directChild) {
            this.parent = parent;
            this.directChild = directChild;
        }
    }

    private static final class MenuTarget {
        final ViewGroup parent;
        final View anchorRow;
        final int index;
        MenuTarget(ViewGroup parent, View anchorRow, int index) {
            this.parent = parent;
            this.anchorRow = anchorRow;
            this.index = index;
        }
    }

    private HostSettingInjector() {}
}
