package com.ace.toolbox.xposed;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class CleanerEngine {
    private static final Set<String> DENY_EXACT = new HashSet<>();
    static {
        String[] names = {"EnMicroMsg.db", "MicroMsg.db", "MM.sqlite", "msg.db", "contact.db", "favorite.db"};
        java.util.Collections.addAll(DENY_EXACT, names);
    }

    static List<CleanModels.Root> roots(Context c, String pkg) {
        List<CleanModels.Root> roots = new ArrayList<>();
        add(roots, "应用缓存", c.getCacheDir());
        add(roots, "外部缓存", c.getExternalCacheDir());
        if (android.os.Build.VERSION.SDK_INT >= 21) add(roots, "代码缓存", c.getCodeCacheDir());
        add(roots, "日志", new File(c.getFilesDir(), "log"));
        add(roots, "日志", new File(c.getFilesDir(), "logs"));
        add(roots, "临时文件", new File(c.getFilesDir(), "tmp"));

        // Only narrowly named disposable directories are added from shared/legacy paths.
        File sd = Environment.getExternalStorageDirectory();
        if (HostPackages.QQ.equals(pkg)) {
            add(roots, "QQ 临时缓存", new File(sd, "Android/data/com.tencent.mobileqq/cache"));
            add(roots, "QQ 临时目录", new File(sd, "Tencent/MobileQQ/.tmp"));
        } else {
            add(roots, "微信临时缓存", new File(sd, "Android/data/com.tencent.mm/cache"));
        }
        return dedupe(roots);
    }

    static CleanModels.Scan scan(Context c, String pkg) {
        CleanModels.Scan s = new CleanModels.Scan();
        for (CleanModels.Root root : roots(c, pkg)) walk(root.file, s, false, null, null);
        s.accounts.addAll(UserDirectoryDetector.detect(pkg));
        return s;
    }

    static CleanModels.Result clean(Context c, String pkg, int totalFiles, CleanModels.Progress progress) {
        CleanModels.Result r = new CleanModels.Result();
        for (CleanModels.Root root : roots(c, pkg)) walk(root.file, null, true, r, progressWithTotal(progress, totalFiles));
        return r;
    }

    private static CleanModels.Progress progressWithTotal(CleanModels.Progress p, int total) {
        if (p == null) return null;
        return (deleted, ignored, freed) -> p.onProgress(deleted, Math.max(total, 1), freed);
    }

    private static void walk(File file, CleanModels.Scan scan, boolean delete, CleanModels.Result result, CleanModels.Progress progress) {
        if (file == null || !file.exists()) return;
        if (file.isFile()) {
            if (!isSafeFile(file)) return;
            long len = safeLength(file);
            if (scan != null) { scan.files++; scan.bytes += len; }
            if (delete && result != null && file.delete()) {
                result.deletedFiles++;
                result.freedBytes += len;
                if (progress != null && (result.deletedFiles % 25 == 0))
                    progress.onProgress(result.deletedFiles, 1, result.freedBytes);
            }
            return;
        }
        File[] children;
        try { children = file.listFiles(); } catch (Throwable t) { return; }
        if (children == null) return;
        for (File child : children) walk(child, scan, delete, result, progress);
        // Keep root directories; remove only empty nested directories.
        if (delete && file.getParentFile() != null) {
            try {
                File[] left = file.listFiles();
                if (left != null && left.length == 0 && isDisposableDirectoryName(file.getName())) file.delete();
            } catch (Throwable ignored) {}
        }
    }

    private static boolean isSafeFile(File f) {
        String n = f.getName();
        if (DENY_EXACT.contains(n)) return false;
        String lower = n.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".db") || lower.endsWith(".sqlite") || lower.endsWith(".sqlite3")) return false;
        String path = f.getAbsolutePath().toLowerCase(Locale.ROOT);
        String[] denied = {"/msg/", "/message/", "/contact/", "/favorite/", "/favourite/", "/account/", "/auth/"};
        for (String d : denied) if (path.contains(d)) return false;
        return true;
    }

    private static boolean isDisposableDirectoryName(String n) {
        String s = n.toLowerCase(Locale.ROOT);
        return s.equals("cache") || s.equals("tmp") || s.equals(".tmp") || s.equals("log") || s.equals("logs") || s.equals("thumb") || s.equals("thumbnail");
    }

    private static long safeLength(File f) { try { return f.length(); } catch (Throwable t) { return 0; } }
    private static void add(List<CleanModels.Root> list, String label, File f) { if (f != null && f.exists()) list.add(new CleanModels.Root(label, f)); }
    private static List<CleanModels.Root> dedupe(List<CleanModels.Root> in) {
        List<CleanModels.Root> out = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (CleanModels.Root r : in) if (seen.add(r.file.getAbsolutePath())) out.add(r);
        return out;
    }
    private CleanerEngine() {}
}
