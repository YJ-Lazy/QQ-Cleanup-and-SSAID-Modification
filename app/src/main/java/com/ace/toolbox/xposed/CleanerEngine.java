package com.ace.toolbox.xposed;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * QQ/WeChat cleanup engine.
 *
 * v0.2.4 adds a QQ-specific selectable path catalog inspired by the publicly visible
 * storage-cleaning path list in oneQAQone/QFun. The implementation here is independently
 * written and keeps ACE's stronger denylist/protection rules.
 */
final class CleanerEngine {
    private static final Pattern QQ_UIN = Pattern.compile("[1-9][0-9]{4,14}");

    private static final Set<String> DENY_EXACT = new HashSet<>();
    private static final Set<String> PROTECTED_PARTS = new HashSet<>();

    static {
        Collections.addAll(
                DENY_EXACT,
                "EnMicroMsg.db", "MicroMsg.db", "MM.sqlite", "msg.db",
                "contact.db", "favorite.db", "favourite.db"
        );

        Collections.addAll(
                PROTECTED_PARTS,
                "databases", "database", "db",
                "shared_prefs", "sharedprefs",
                "account", "accounts", "auth", "login", "token", "session",
                "msg", "message", "messages",
                "contact", "contacts",
                "favorite", "favourite", "favorites", "favourites",
                "wallet", "pay", "payment",
                "backup", "keystore", "key_store"
        );
    }

    static List<CleanModels.Target> targets(Context c, String pkg) {
        if (HostPackages.QQ.equals(pkg)) return qqTargets(c);
        return genericWechatTargets(c);
    }

    static List<CleanModels.TargetScan> scanTargets(Context c, String pkg) {
        List<CleanModels.TargetScan> out = new ArrayList<>();
        for (CleanModels.Target target : targets(c, pkg)) {
            CleanModels.TargetScan ts = new CleanModels.TargetScan(target);
            for (File root : dedupeFiles(target.roots)) {
                walk(root, ts, false, null, null);
            }
            out.add(ts);
        }
        return out;
    }

    static CleanModels.Result cleanTargets(
            List<CleanModels.TargetScan> selected,
            CleanModels.Progress progress
    ) {
        int totalFiles = 0;
        for (CleanModels.TargetScan s : selected) totalFiles += s.files;

        CleanModels.Result result = new CleanModels.Result();
        for (CleanModels.TargetScan s : selected) {
            for (File root : dedupeFiles(s.target.roots)) {
                walk(
                        root,
                        null,
                        true,
                        result,
                        progressWithTotal(progress, totalFiles)
                );
            }
        }
        return result;
    }

    // Backward-compatible aggregate API.
    static CleanModels.Scan scan(Context c, String pkg) {
        CleanModels.Scan s = new CleanModels.Scan();
        for (CleanModels.TargetScan ts : scanTargets(c, pkg)) {
            s.bytes += ts.bytes;
            s.files += ts.files;
        }
        s.accounts.addAll(UserDirectoryDetector.detect(pkg));
        return s;
    }

    static CleanModels.Result clean(
            Context c,
            String pkg,
            int totalFiles,
            CleanModels.Progress progress
    ) {
        List<CleanModels.TargetScan> selected = scanTargets(c, pkg);
        return cleanTargets(selected, progress);
    }

    private static List<CleanModels.Target> qqTargets(Context c) {
        List<CleanModels.Target> out = new ArrayList<>();

        File privateRoot = Build.VERSION.SDK_INT >= 24
                ? c.getDataDir()
                : c.getFilesDir().getParentFile();

        File externalAppRoot = null;
        try {
            File ef = c.getExternalFilesDir(null);
            if (ef != null) externalAppRoot = ef.getParentFile();
        } catch (Throwable ignored) {}

        File legacyRoot = Environment.getExternalStorageDirectory();

        // ---- Basic / safe defaults ----
        addTarget(out, target(
                "std_internal_cache", "基础缓存", "内部 Cache",
                "Android 标准内部缓存目录", true, false,
                c.getCacheDir()
        ));

        addTarget(out, target(
                "std_external_cache", "基础缓存", "外部 Cache",
                "Android 标准外部缓存目录", true, false,
                c.getExternalCacheDir()
        ));

        if (Build.VERSION.SDK_INT >= 21) {
            addTarget(out, target(
                    "code_cache", "基础缓存", "代码缓存",
                    "ART / WebView 代码缓存", true, false,
                    c.getCodeCacheDir()
            ));
        }

        addTarget(out, target(
                "x5_cache", "基础缓存", "X5 / WebView 缓存",
                "QQ 内置浏览器网页缓存，不删除 Cookie/登录数据", true, false,
                child(privateRoot, "app_x5webview/Cache"),
                child(privateRoot, "app_webview/Default/Cache"),
                child(privateRoot, "app_webview/Default/Code Cache"),
                child(privateRoot, "app_webview/Default/GPUCache")
        ));

        addTarget(out, target(
                "tbs", "基础缓存", "TBS 内核缓存",
                "TBS/X5 临时缓存及日志", true, false,
                child(externalAppRoot, "files/tbs"),
                child(externalAppRoot, "files/commonlog")
        ));

        addTarget(out, target(
                "app_logs", "日志与临时文件", "QQ 日志",
                "运行日志、单进程日志与临时日志", true, false,
                child(externalAppRoot, "files/onelog"),
                child(externalAppRoot, "files/tencent"),
                child(privateRoot, "files/log"),
                child(privateRoot, "files/logs"),
                child(privateRoot, "files/crash"),
                child(privateRoot, "files/crashes")
        ));

        addTarget(out, target(
                "opensdk_tmp", "日志与临时文件", "OpenSDK 临时文件",
                "第三方登录/分享 SDK 临时缓存", true, false,
                child(externalAppRoot, "files/opensdk_tmp")
        ));

        addTarget(out, target(
                "ark_cache", "功能缓存", "Ark 卡片缓存",
                "JSON/Ark 卡片渲染缓存", true, false,
                child(privateRoot, "files/ArkApp/Cache")
        ));

        addTarget(out, target(
                "maps", "功能缓存", "地图缓存",
                "腾讯地图 SDK 地图瓦片与临时资源", true, false,
                child(externalAppRoot, "files/tencentmapsdk")
        ));

        addTarget(out, target(
                "guild_qcircle", "功能缓存", "频道 / 看点缓存",
                "频道、QCircle/看点图片及临时资源", true, false,
                child(externalAppRoot, "files/guild"),
                child(externalAppRoot, "files/qcircle"),
                child(externalAppRoot, "qcircle")
        ));

        addTarget(out, target(
                "qqshow", "功能缓存", "QQ秀资源缓存",
                "超级 QQ 秀、Zootopia 下载资源", true, false,
                child(externalAppRoot, "files/QQShowDownload"),
                child(externalAppRoot, "files/zootopia_download"),
                child(legacyRoot, "Tencent/MobileQQ/.apollo")
        ));

        addTarget(out, target(
                "appearance", "功能缓存", "个性装扮缓存",
                "头像框、字体、名片、戳一戳、群特效等可重新下载资源", true, false,
                child(legacyRoot, "Tencent/MobileQQ/.card"),
                child(legacyRoot, "Tencent/MobileQQ/.CorlorNick"),
                child(legacyRoot, "Tencent/MobileQQ/.font_effect"),
                child(legacyRoot, "Tencent/MobileQQ/.font_info"),
                child(legacyRoot, "Tencent/MobileQQ/.pendant"),
                child(legacyRoot, "Tencent/MobileQQ/.profilecard"),
                child(legacyRoot, "Tencent/MobileQQ/.troop"),
                child(legacyRoot, "Tencent/MobileQQ/.vaspoke"),
                child(privateRoot, "files/bubble_info"),
                child(privateRoot, "files/vas_material_folder")
        ));

        addTarget(out, target(
                "mini_cache", "功能缓存", "小程序缓存",
                "小程序运行缓存、日志和 SDK 临时资源；不删除账号/数据库", false, true,
                child(externalAppRoot, "Tencent/mini"),
                child(externalAppRoot, "Tencent/wxminiapp")
        ));

        // ---- Media caches: useful but intentionally opt-in ----
        addTarget(out, target(
                "chatpic", "聊天媒体缓存", "聊天图片缓存",
                "已下载聊天图片缓存；历史消息记录不会删除，但图片可能需要重新下载", false, true,
                child(legacyRoot, "Tencent/MobileQQ/chatpic"),
                child(externalAppRoot, "Tencent/MobileQQ/chatpic")
        ));

        addTarget(out, target(
                "diskcache", "聊天媒体缓存", "图片磁盘缓存",
                "QQ 图片磁盘缓存，可重新生成", false, true,
                child(legacyRoot, "Tencent/MobileQQ/diskcache"),
                child(externalAppRoot, "Tencent/MobileQQ/diskcache")
        ));

        addTarget(out, target(
                "thumb", "聊天媒体缓存", "缩略图缓存",
                "聊天与媒体缩略图，可重新生成", false, true,
                child(legacyRoot, "Tencent/MobileQQ/thumb"),
                child(externalAppRoot, "Tencent/MobileQQ/thumb")
        ));

        addTarget(out, target(
                "head_hotpic", "聊天媒体缓存", "头像 / 热图缓存",
                "头像和热门图片缓存，可重新下载", false, true,
                child(legacyRoot, "Tencent/MobileQQ/head"),
                child(legacyRoot, "Tencent/MobileQQ/hotpic"),
                child(externalAppRoot, "Tencent/MobileQQ/head"),
                child(externalAppRoot, "Tencent/MobileQQ/hotpic")
        ));

        addTarget(out, target(
                "photo", "聊天媒体缓存", "照片预发送缓存",
                "发送图片前的临时处理文件", false, true,
                child(legacyRoot, "Tencent/MobileQQ/photo"),
                child(externalAppRoot, "Tencent/MobileQQ/photo")
        ));

        addTarget(out, target(
                "shortvideo", "聊天媒体缓存", "短视频缓存",
                "已缓存短视频和临时视频资源", false, true,
                child(legacyRoot, "Tencent/MobileQQ/shortvideo"),
                child(externalAppRoot, "Tencent/MobileQQ/shortvideo")
        ));

        addTarget(out, target(
                "scribble", "聊天媒体缓存", "涂鸦 / 作图缓存",
                "涂鸦、作图与长截图临时资源", false, true,
                child(legacyRoot, "Tencent/MobileQQ/Scribble"),
                child(legacyRoot, "Tencent/MobileQQ/zhitu"),
                child(legacyRoot, "Tencent/MobileQQ/aio_long_shot"),
                child(externalAppRoot, "Tencent/MobileQQ/Scribble"),
                child(externalAppRoot, "Tencent/MobileQQ/zhitu"),
                child(externalAppRoot, "Tencent/MobileQQ/aio_long_shot")
        ));

        // Per-account PTT cache. Each numeric UIN becomes part of the same selectable target.
        CleanModels.Target ptt = new CleanModels.Target(
                "ptt",
                "聊天媒体缓存",
                "语音缓存",
                "各 QQ 账号的 ptt 语音缓存；清理后旧语音可能需要重新下载",
                false,
                true
        );
        collectUinPtt(ptt.roots, child(legacyRoot, "Tencent/MobileQQ"));
        collectUinPtt(ptt.roots, child(externalAppRoot, "Tencent/MobileQQ"));
        addTarget(out, ptt);

        // Deliberately NOT added:
        // QWallet, QQ_Favorite, arbitrary files/mini, pddata, databases, shared_prefs.
        return out;
    }

    private static List<CleanModels.Target> genericWechatTargets(Context c) {
        List<CleanModels.Target> out = new ArrayList<>();

        addTarget(out, target(
                "wx_cache", "基础缓存", "微信应用缓存",
                "Android 标准缓存目录", true, false,
                c.getCacheDir(), c.getExternalCacheDir()
        ));

        if (Build.VERSION.SDK_INT >= 21) {
            addTarget(out, target(
                    "wx_code_cache", "基础缓存", "代码缓存",
                    "Android 代码缓存", true, false,
                    c.getCodeCacheDir()
            ));
        }

        addTarget(out, target(
                "wx_logs", "日志与临时文件", "日志 / 临时文件",
                "微信日志与临时目录；数据库和消息路径仍受保护", true, false,
                child(c.getFilesDir(), "log"),
                child(c.getFilesDir(), "logs"),
                child(c.getFilesDir(), "tmp")
        ));
        return out;
    }

    private static CleanModels.Target target(
            String id,
            String category,
            String label,
            String description,
            boolean defaultSelected,
            boolean deepClean,
            File... files
    ) {
        CleanModels.Target t = new CleanModels.Target(
                id, category, label, description, defaultSelected, deepClean
        );
        if (files != null) {
            for (File f : files) {
                if (f != null) t.roots.add(f);
            }
        }
        return t;
    }

    private static void addTarget(
            List<CleanModels.Target> out,
            CleanModels.Target target
    ) {
        if (target == null || target.roots.isEmpty()) return;

        List<File> valid = dedupeFiles(target.roots);
        target.roots.clear();
        target.roots.addAll(valid);

        // Keep targets even when paths don't exist yet. UI can show 0 B and stays stable.
        out.add(target);
    }

    private static void collectUinPtt(List<File> out, File mobileQqRoot) {
        if (mobileQqRoot == null) return;
        try {
            File[] dirs = mobileQqRoot.listFiles();
            if (dirs == null) return;
            for (File dir : dirs) {
                if (dir != null
                        && dir.isDirectory()
                        && QQ_UIN.matcher(dir.getName()).matches()) {
                    out.add(new File(dir, "ptt"));
                }
            }
        } catch (Throwable ignored) {}
    }

    private static File child(File parent, String relative) {
        if (parent == null || relative == null) return null;
        return new File(parent, relative);
    }

    private static void walk(
            File file,
            CleanModels.TargetScan scan,
            boolean delete,
            CleanModels.Result result,
            CleanModels.Progress progress
    ) {
        if (file == null || !file.exists()) return;
        if (isProtectedPath(file)) return;

        if (file.isFile()) {
            if (!isSafeFile(file)) return;

            long len = safeLength(file);
            if (scan != null) {
                scan.files++;
                scan.bytes += len;
            }

            if (delete && result != null) {
                boolean removed = false;
                try {
                    removed = file.delete();
                } catch (Throwable ignored) {}

                if (removed) {
                    result.deletedFiles++;
                    result.freedBytes += len;
                    if (progress != null && result.deletedFiles % 20 == 0) {
                        progress.onProgress(result.deletedFiles, 1, result.freedBytes);
                    }
                } else {
                    result.failedFiles++;
                }
            }
            return;
        }

        File[] children;
        try {
            children = file.listFiles();
        } catch (Throwable t) {
            return;
        }
        if (children == null) return;

        for (File child : children) {
            walk(child, scan, delete, result, progress);
        }

        if (delete && file.getParentFile() != null) {
            try {
                File[] left = file.listFiles();
                if (left != null && left.length == 0) {
                    // Keep well-known top-level roots themselves; deleting nested empty folders is OK.
                    String name = file.getName().toLowerCase(Locale.ROOT);
                    if (!"mobileqq".equals(name)
                            && !"tencent".equals(name)
                            && !"files".equals(name)) {
                        file.delete();
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    private static CleanModels.Progress progressWithTotal(
            CleanModels.Progress p,
            int total
    ) {
        if (p == null) return null;
        return (deleted, ignored, freed) ->
                p.onProgress(deleted, Math.max(total, 1), freed);
    }

    private static boolean isSafeFile(File f) {
        String name = f.getName();
        if (DENY_EXACT.contains(name)) return false;

        String lower = name.toLowerCase(Locale.ROOT);
        return !lower.endsWith(".db")
                && !lower.endsWith(".sqlite")
                && !lower.endsWith(".sqlite3")
                && !lower.endsWith(".db-wal")
                && !lower.endsWith(".db-shm")
                && !isProtectedPath(f);
    }

    private static boolean isProtectedPath(File f) {
        String path;
        try {
            path = f.getCanonicalPath().toLowerCase(Locale.ROOT).replace('\\', '/');
        } catch (Throwable t) {
            path = f.getAbsolutePath().toLowerCase(Locale.ROOT).replace('\\', '/');
        }

        for (String part : PROTECTED_PARTS) {
            if (path.contains("/" + part + "/")) return true;
        }
        return false;
    }

    private static long safeLength(File f) {
        try {
            return f.length();
        } catch (Throwable t) {
            return 0L;
        }
    }

    private static List<File> dedupeFiles(List<File> input) {
        ArrayList<File> sorted = new ArrayList<>();
        for (File f : input) {
            if (f != null) sorted.add(f);
        }

        sorted.sort(Comparator.comparingInt(f -> f.getAbsolutePath().length()));

        ArrayList<File> out = new ArrayList<>();
        ArrayList<String> seen = new ArrayList<>();

        for (File f : sorted) {
            String path;
            try {
                path = f.getCanonicalPath();
            } catch (Throwable t) {
                path = f.getAbsolutePath();
            }

            boolean covered = false;
            for (String parent : seen) {
                if (path.equals(parent)
                        || path.startsWith(parent + File.separator)) {
                    covered = true;
                    break;
                }
            }

            if (!covered) {
                seen.add(path);
                out.add(f);
            }
        }
        return out;
    }

    private CleanerEngine() {}
}
