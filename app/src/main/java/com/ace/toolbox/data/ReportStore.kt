package com.ace.toolbox.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CleanReport(val packageName: String, val hostVersion: String, val time: Long, val before: Long, val after: Long, val deleted: Int, val freed: Long)

object ReportStore {
    fun load(context: Context): List<CleanReport> {
        val raw = context.getSharedPreferences("ace_reports", 0).getString("items", "").orEmpty()
        return raw.lines().mapNotNull { line ->
            val p = line.split('|')
            if (p.size != 7) null else runCatching {
                CleanReport(p[0], p[1], p[2].toLong(), p[3].toLong(), p[4].toLong(), p[5].toInt(), p[6].toLong())
            }.getOrNull()
        }
    }
    fun time(ts: Long): String = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
    fun size(v: Long): String {
        if (v < 1024) return "$v B"
        var n = v.toDouble(); val units = arrayOf("KB","MB","GB","TB"); var i = -1
        do { n /= 1024; i++ } while (n >= 1024 && i < units.lastIndex)
        return String.format(Locale.getDefault(), "%.1f %s", n, units[i])
    }
}
