package com.ace.toolbox.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder

class ConfigProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "com.ace.toolbox.config"
        private const val CONFIG = 1
        private const val REPORT = 2
        private const val REPORTS = 3
        private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "config", CONFIG)
            addURI(AUTHORITY, "report", REPORT)
            addURI(AUTHORITY, "reports", REPORTS)
        }
    }

    private val prefs get() = requireNotNull(context).getSharedPreferences("ace_config", 0)
    private val reports get() = requireNotNull(context).getSharedPreferences("ace_reports", 0)

    override fun onCreate() = true

    private fun enforceAllowedCaller() {
        val ctx = requireNotNull(context)
        val uid = Binder.getCallingUid()
        if (uid == android.os.Process.myUid()) return
        val packages = ctx.packageManager.getPackagesForUid(uid)?.toSet().orEmpty()
        if ("com.tencent.mobileqq" !in packages && "com.tencent.mm" !in packages) {
            throw SecurityException("Caller is outside ACE static scope")
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        enforceAllowedCaller()
        return when (matcher.match(uri)) {
            CONFIG -> MatrixCursor(
                arrayOf(
                    "clean_enabled",
                    "compat_mode",
                    "rule_base_url",
                    "ssaid_enabled",
                    "ssaid_value"
                )
            ).apply {
                addRow(
                    arrayOf(
                        if (prefs.getBoolean("clean_enabled", true)) 1 else 0,
                        if (prefs.getBoolean("compat_mode", true)) 1 else 0,
                        prefs.getString("rule_base_url", "") ?: "",
                        if (prefs.getBoolean("ssaid_enabled", false)) 1 else 0,
                        prefs.getString("ssaid_value", "") ?: ""
                    )
                )
            }
            REPORTS -> reportCursor()
            else -> throw IllegalArgumentException("Unknown uri: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        enforceAllowedCaller()
        if (matcher.match(uri) != REPORT || values == null) throw IllegalArgumentException("Unsupported insert")
        val pkg = values.getAsString("package_name") ?: return null
        if (pkg != "com.tencent.mobileqq" && pkg != "com.tencent.mm") return null
        val ts = values.getAsLong("timestamp") ?: System.currentTimeMillis()
        val row = listOf(
            pkg,
            values.getAsString("host_version") ?: "?",
            ts.toString(),
            (values.getAsLong("before_bytes") ?: 0L).toString(),
            (values.getAsLong("after_bytes") ?: 0L).toString(),
            (values.getAsInteger("deleted_files") ?: 0).toString(),
            (values.getAsLong("freed_bytes") ?: 0L).toString()
        ).joinToString("|")
        val current = reports.getString("items", "")?.lines()?.filter { it.isNotBlank() }.orEmpty()
        val next = (listOf(row) + current).take(30).joinToString("\n")
        reports.edit().putString("items", next).apply()
        return Uri.withAppendedPath(Uri.parse("content://$AUTHORITY/reports"), ts.toString())
    }

    private fun reportCursor(): Cursor = MatrixCursor(
        arrayOf(
            "package_name",
            "host_version",
            "timestamp",
            "before_bytes",
            "after_bytes",
            "deleted_files",
            "freed_bytes"
        )
    ).apply {
        reports.getString("items", "")?.lines()?.filter { it.isNotBlank() }?.forEach { line ->
            val p = line.split('|')
            if (p.size == 7) addRow(p.toTypedArray())
        }
    }

    override fun getType(uri: Uri) = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
}
