package com.ace.toolbox.xposed;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class UserDirectoryDetector {
    private static final Pattern QQ_UIN = Pattern.compile("[1-9][0-9]{4,14}");
    private static final Pattern WX_HASH = Pattern.compile("[0-9a-fA-F]{24,40}");

    static List<String> detect(String pkg) {
        List<String> out = new ArrayList<>();
        File storage = Environment.getExternalStorageDirectory();
        if (HostPackages.QQ.equals(pkg)) {
            scanNames(new File(storage, "Tencent/MobileQQ"), QQ_UIN, "QQ ", out);
            scanNames(new File(storage, "Android/data/com.tencent.mobileqq/Tencent/MobileQQ"), QQ_UIN, "QQ ", out);
        } else if (HostPackages.WECHAT.equals(pkg)) {
            scanNames(new File(storage, "tencent/MicroMsg"), WX_HASH, "微信目录 ", out);
            scanNames(new File(storage, "Android/data/com.tencent.mm/MicroMsg"), WX_HASH, "微信目录 ", out);
        }
        return out;
    }

    private static void scanNames(File dir, Pattern pattern, String prefix, List<String> out) {
        try {
            File[] files = dir.listFiles();
            if (files == null) return;
            for (File f : files) {
                if (f.isDirectory() && pattern.matcher(f.getName()).matches()) {
                    String label = prefix + mask(f.getName());
                    if (!out.contains(label)) out.add(label);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static String mask(String s) {
        if (s.length() <= 8) return s;
        return s.substring(0, 4) + "…" + s.substring(s.length() - 4);
    }

    private UserDirectoryDetector() {}
}
