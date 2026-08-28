package com.ace.toolbox.xposed;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;

final class HostCleanerDialog {
    static void show(Activity activity, String pkg) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(activity, 24), dp(activity, 8), dp(activity, 24), 0);

        TextView status = new TextView(activity);
        status.setText("正在扫描安全缓存…");
        status.setTextSize(15);
        status.setPadding(0, dp(activity, 8), 0, dp(activity, 12));
        ProgressBar bar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        bar.setIndeterminate(true);
        box.addView(status, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 8)));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("ACE 安全清理")
                .setView(box)
                .setNegativeButton("关闭", null)
                .create();
        dialog.setOnShowListener(d -> scan(activity, pkg, dialog, status, bar));
        dialog.show();
    }

    private static void scan(Activity activity, String pkg, AlertDialog dialog, TextView status, ProgressBar bar) {
        new Thread(() -> {
            CleanModels.Scan scan = CleanerEngine.scan(activity, pkg);
            activity.runOnUiThread(() -> {
                String accounts = scan.accounts.isEmpty() ? "未发现可识别用户目录" : android.text.TextUtils.join("、", scan.accounts);
                status.setText("可安全清理：" + formatBytes(scan.bytes) + "\n文件：" + scan.files + " 个\n识别：" + accounts +
                        "\n\n默认仅处理 cache / tmp / log 等目录，并跳过数据库、消息、联系人、收藏和账号数据。");
                bar.setIndeterminate(false);
                bar.setProgress(0);
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, "开始清理", (d, which) -> confirmClean(activity, pkg, scan));
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
            });
        }, "ACE-scan").start();
    }

    private static void confirmClean(Activity activity, String pkg, CleanModels.Scan scan) {
        new AlertDialog.Builder(activity)
                .setTitle("确认清理")
                .setMessage("将清理 " + formatBytes(scan.bytes) + " 的安全缓存。不会主动删除聊天数据库和用户目录。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清理", (d, w) -> doClean(activity, pkg, scan))
                .show();
    }

    private static void doClean(Activity activity, String pkg, CleanModels.Scan scan) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(activity, 24), dp(activity, 12), dp(activity, 24), 0);
        ProgressBar p = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(Math.max(scan.files, 1));
        TextView t = new TextView(activity);
        t.setText("正在清理…");
        t.setGravity(Gravity.START);
        box.addView(t);
        box.addView(p, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 8)));
        AlertDialog dlg = new AlertDialog.Builder(activity).setTitle("清理进度").setView(box).setCancelable(false).create();
        dlg.show();

        new Thread(() -> {
            CleanModels.Result result = CleanerEngine.clean(activity, pkg, scan.files, (deleted, total, freed) ->
                    activity.runOnUiThread(() -> {
                        p.setProgress(Math.min(deleted, p.getMax()));
                        t.setText("已处理 " + deleted + "/" + total + " · 已释放 " + formatBytes(freed));
                    }));
            CleanModels.Scan after = CleanerEngine.scan(activity, pkg);
            HostReportBridge.submit(activity, pkg, scan.bytes, after.bytes, result.deletedFiles, result.freedBytes);
            activity.runOnUiThread(() -> {
                dlg.dismiss();
                new AlertDialog.Builder(activity)
                        .setTitle("清理完成")
                        .setMessage("释放：" + formatBytes(result.freedBytes) + "\n删除：" + result.deletedFiles + " 个文件\n剩余安全缓存：" + formatBytes(after.bytes))
                        .setPositiveButton("完成", null).show();
            });
        }, "ACE-clean").start();
    }

    static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double v = bytes; String[] u = {"KB","MB","GB","TB"}; int i = -1;
        do { v /= 1024.0; i++; } while (v >= 1024 && i < u.length - 1);
        return String.format(Locale.getDefault(), "%.1f %s", v, u[i]);
    }

    private static int dp(Activity a, int v) { return (int)(v * a.getResources().getDisplayMetrics().density + .5f); }
    private HostCleanerDialog() {}
}
