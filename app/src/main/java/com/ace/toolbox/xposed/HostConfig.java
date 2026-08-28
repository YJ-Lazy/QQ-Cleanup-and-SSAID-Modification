package com.ace.toolbox.xposed;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

final class HostConfig {
    private static final Uri CONFIG_URI = Uri.parse("content://com.ace.toolbox.config/config");

    static boolean cleanEnabled(Context context) {
        try (Cursor c = context.getContentResolver().query(CONFIG_URI, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex("clean_enabled");
                return i < 0 || c.getInt(i) == 1;
            }
        } catch (Throwable ignored) {}
        return true;
    }

    private HostConfig() {}
}
