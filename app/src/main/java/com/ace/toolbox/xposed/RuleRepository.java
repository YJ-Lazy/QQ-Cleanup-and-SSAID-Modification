package com.ace.toolbox.xposed;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class RuleRepository {
    private static final String TAG = "ACE-Rules";
    private static final Uri CONFIG_URI = Uri.parse("content://com.ace.toolbox.config/config");

    static HookRule resolveCachedOrBundled(Context context, String pkg) {
        String version = versionName(context, pkg);
        HookRule cached = loadCached(context, pkg, version);
        if (cached != null) return cached;
        return loadBundled(pkg);
    }

    static HookRule loadBundled(String pkg) {
        String file = HostPackages.QQ.equals(pkg) ? "rules/qq.json" : "rules/wechat.json";
        try (InputStream in = ModuleMain.class.getClassLoader().getResourceAsStream(file)) {
            if (in == null) return null;
            return parse(readAll(in), pkg);
        } catch (Throwable t) {
            Log.e(TAG, "Bundled rule error", t);
            return null;
        }
    }

    static HookRule fetchRemote(Context context, String pkg) {
        try {
            String base = readConfig(context, "rule_base_url");
            if (base == null || base.trim().isEmpty()) return null;
            while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            String version = versionName(context, pkg);
            String family = HostPackages.QQ.equals(pkg) ? "qq" : "wechat";
            HookRule rule = download(base + "/" + family + "/" + version + ".json", pkg);
            if (rule == null) rule = download(base + "/" + family + "/default.json", pkg);
            if (rule != null) saveCached(context, pkg, version, rule);
            return rule;
        } catch (Throwable t) {
            Log.w(TAG, "Remote rule refresh failed: " + t.getMessage());
            return null;
        }
    }

    private static HookRule download(String url, String pkg) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(6000);
            c.setReadTimeout(6000);
            c.setRequestProperty("User-Agent", "ACE-Toolkit/0.1");
            if (c.getResponseCode() != 200) return null;
            return parse(readAll(c.getInputStream()), pkg);
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static HookRule parse(String raw, String expectedPkg) throws Exception {
        JSONObject o = new JSONObject(raw);
        int schema = o.optInt("schema", 1);
        String pkg = o.getString("package");
        if (!expectedPkg.equals(pkg) || schema != 1) throw new IllegalArgumentException("Invalid rule schema/package");
        String method = o.optString("method", "onCreate");
        if (!"onCreate".equals(method)) throw new IllegalArgumentException("Only onCreate is accepted in v0.1");
        JSONArray a = o.getJSONArray("settings");
        List<String> classes = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            String n = a.getString(i).trim();
            // Remote rules can name only application classes; never allow framework/system targets here.
            if (n.startsWith("com.tencent.") && n.length() < 180) classes.add(n);
        }
        if (classes.isEmpty()) throw new IllegalArgumentException("Rule has no safe setting candidates");
        return new HookRule(schema, pkg, o.optString("version", "*"), method, classes);
    }

    private static HookRule loadCached(Context context, String pkg, String version) {
        File f = cacheFile(context, pkg, version);
        if (!f.isFile()) return null;
        try (InputStream in = new FileInputStream(f)) { return parse(readAll(in), pkg); }
        catch (Throwable ignored) { return null; }
    }

    private static void saveCached(Context context, String pkg, String version, HookRule rule) throws Exception {
        File f = cacheFile(context, pkg, version);
        File parent = f.getParentFile();
        if (parent != null) parent.mkdirs();
        JSONObject o = new JSONObject();
        o.put("schema", rule.schema);
        o.put("package", rule.packageName);
        o.put("version", rule.version);
        o.put("method", rule.methodName);
        o.put("settings", new JSONArray(rule.settingClasses));
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(o.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static File cacheFile(Context context, String pkg, String version) {
        String safe = version.replaceAll("[^0-9A-Za-z._-]", "_");
        return new File(new File(context.getFilesDir(), "ace_toolkit/rules"), pkg + "_" + safe + ".json");
    }

    private static String versionName(Context context, String pkg) {
        try {
            PackageInfo p = context.getPackageManager().getPackageInfo(pkg, 0);
            return p.versionName == null ? "unknown" : p.versionName;
        } catch (Throwable ignored) { return "unknown"; }
    }

    private static String readConfig(Context context, String column) {
        try (Cursor c = context.getContentResolver().query(CONFIG_URI, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(column);
                if (i >= 0 && !c.isNull(i)) return c.getString(i);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static String readAll(InputStream in) throws Exception {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder b = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) b.append(line).append('\n');
        return b.toString();
    }

    private RuleRepository() {}
}
