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

    static String ssaidValue(Context context) {
        try (Cursor c = query(context)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex("ssaid_value");
                if (i >= 0 && !c.isNull(i)) {
                    String value = c.getString(i);
                    return value == null ? "" : value.trim().toLowerCase();
                }
            }
        } catch (Throwable ignored) {}
        return "";
    }


static boolean javaScriptEnabled(Context context) {
    try (Cursor c = query(context)) {
        if (c != null && c.moveToFirst()) {
            int i = c.getColumnIndex("java_script_enabled");
            return i >= 0 && c.getInt(i) == 1;
        }
    } catch (Throwable ignored) {}
    return false;
}

static String javaScriptSource(Context context) {
    try (Cursor c = query(context)) {
        if (c != null && c.moveToFirst()) {
            int i = c.getColumnIndex("java_script_source");
            if (i >= 0 && !c.isNull(i)) {
                String value = c.getString(i);
                return value == null ? "" : value;
            }
        }
    } catch (Throwable ignored) {}
    return "";
}


    static boolean javaScriptAutoRun(Context context) {
        try (Cursor c = query(context)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex("java_script_auto_run");
                return i >= 0 && c.getInt(i) == 1;
            }
        } catch (Throwable ignored) {}
        return false;
    }


    static boolean javaScriptReceiveCallback(Context context) {
        return boolColumn(context, "java_script_receive_callback");
    }

    static boolean javaScriptSendCallback(Context context) {
        return boolColumn(context, "java_script_send_callback");
    }

    static boolean javaScriptGroupCallback(Context context) {
        return boolColumn(context, "java_script_group_callback");
    }

    private static boolean boolColumn(Context context, String column) {
        try (Cursor c = query(context)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(column);
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
