package com.ace.toolbox.xposed;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.github.libxposed.api.XposedModule;

/**
 * Read-only QQ compatibility diagnostics.
 *
 * This class deliberately does not hide, patch, block, spoof, or alter QQ security checks.
 * It only probes whether selected compatibility targets are present so ACE can degrade safely
 * when QQ changes implementation details between versions.
 */
final class QqCompatibilityProbe {
    private static final String TAG = "ACE-QQ-Compat";

    private static final String[] CLASS_PROBES = new String[] {
            "com.tencent.mobileqq.setting.main.MainSettingFragment",
            "com.tencent.mobileqq.msf.core.MsfCore",
            "com.tencent.gathererga.core.UserInfoImpl",
            "org.light.device.LightDeviceUtils",
            "com.tencent.bugly.proguard.cp",
            "com.tenpay.charge.v2.util.ChargeV2Utils"
    };

    static void run(XposedModule module, ClassLoader loader) {
        List<String> present = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String name : CLASS_PROBES) {
            if (classExists(name, loader)) {
                present.add(name);
            } else {
                missing.add(name);
            }
        }

        module.log(Log.INFO, TAG,
                "QQ compatibility probe complete; present=" + present.size()
                        + "; missing=" + missing.size());

        for (String name : present) {
            module.log(Log.INFO, TAG, "probe present: " + name);
        }
        for (String name : missing) {
            module.log(Log.WARN, TAG, "probe missing: " + name);
        }
    }

    static boolean classExists(String name, ClassLoader loader) {
        try {
            Class.forName(name, false, loader);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private QqCompatibilityProbe() {}
}
