package com.ace.toolbox.xposed;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

final class HostReportBridge {
    private static final Uri REPORT_URI = Uri.parse("content://com.ace.toolbox.config/report");

    static void submit(Context c, String pkg, long before, long after, int deleted, long freed) {
        try {
            ContentValues v = new ContentValues();
            v.put("package_name", pkg);
            v.put("host_version", c.getPackageManager().getPackageInfo(pkg, 0).versionName);
            v.put("timestamp", System.currentTimeMillis());
            v.put("before_bytes", before);
            v.put("after_bytes", after);
            v.put("deleted_files", deleted);
            v.put("freed_bytes", freed);
            c.getContentResolver().insert(REPORT_URI, v);
        } catch (Throwable ignored) {}
    }
    private HostReportBridge() {}
}
