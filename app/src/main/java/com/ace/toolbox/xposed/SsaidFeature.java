package com.ace.toolbox.xposed;

import android.app.Application;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

/** Optional QQ-only feature adapted from / inspired by the public MIT project YJ-Lazy/ssaid-qq.
 * Upstream: https://github.com/YJ-Lazy/ssaid-qq
 * Copyright (c) 2026 YJ-Lazy, MIT License. See NOTICE.md and third_party/ssaid-qq-LICENSE.txt.
 * Disabled by default. It substitutes Settings.Secure.ANDROID_ID only inside QQ's scoped process.
 */
final class SsaidFeature {
    private static final Uri CONFIG_URI = Uri.parse("content://com.ace.toolbox.config/config");

    static void install(XposedModule module, Application app, ClassLoader loader) {
        // Do not install another Settings.Secure hook at all unless the user explicitly enables
        // SSAID. This lowers the chance of interacting with unrelated privacy/device-id modules.
        if (!HostConfig.ssaidEnabled(app)) {
            Log.i("ACE-SSAID", "SSAID disabled; hook not installed");
            return;
        }
        try {
            Method getString = Settings.Secure.class.getDeclaredMethod("getString", ContentResolver.class, String.class);
            module.hook(getString)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (!Settings.Secure.ANDROID_ID.equals(chain.getArg(1))) return chain.proceed();
                        String value = readConfiguredSsaid((ContentResolver) chain.getArg(0));
                        return value == null ? chain.proceed() : value;
                    });
        } catch (Throwable t) {
            Log.w("ACE-SSAID", "SSAID feature unavailable: " + t.getMessage());
        }
    }

    private static String readConfiguredSsaid(ContentResolver resolver) {
        try (Cursor c = resolver.query(CONFIG_URI, null, null, null, null)) {
            if (c == null || !c.moveToFirst()) return null;
            int enabled = c.getColumnIndex("ssaid_enabled");
            int value = c.getColumnIndex("ssaid_value");
            if (enabled < 0 || value < 0 || c.getInt(enabled) != 1 || c.isNull(value)) return null;
            String v = c.getString(value).trim().toLowerCase();
            return v.matches("[0-9a-f]{16}") ? v : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private SsaidFeature() {}
}
