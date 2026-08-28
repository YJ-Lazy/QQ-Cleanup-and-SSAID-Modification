package com.ace.toolbox.xposed;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

final class HostConfig {
    private static final Uri CONFIG_URI = Uri.parse("content://com.ace.toolbox.config/config");

    static boolean cleanEnabled(Context context) {
        try (Cursor c = query(context)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex("clean_enabled");
                return i < 0 || c.getInt(i) == 1;
            }
        } catch (Throwable ignored) {}
        return true;
    }

    static boolean compatibilityMode(Context context) {
        try (Cursor c = query(context)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex("compat_mode");
                return i < 0 || c.getInt(i) == 1;
            }
        } catch (Throwable ignored) {}
        // Safe-by-default when provider is temporarily unavailable.
        return true;
    }

    static boolean ssaidEnabled(Context context) {
        try (Cursor c = query(context)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex("ssaid_enabled");
                return i >= 0 && c.getInt(i) == 1;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static Cursor query(Context context) {
        return context.getContentResolver().query(CONFIG_URI, null, null, null, null);
    }

    private HostConfig() {}
}
