package com.ace.toolbox.data

import android.content.Context
import android.content.pm.PackageManager

data class HostApp(val packageName: String, val displayName: String, val installed: Boolean, val version: String?)

object AppDetector {
    fun detect(context: Context): List<HostApp> = listOf(
        info(context, "com.tencent.mobileqq", "QQ"),
        info(context, "com.tencent.mm", "微信")
    )
    private fun info(context: Context, pkg: String, name: String): HostApp = try {
        val p = context.packageManager.getPackageInfo(pkg, 0)
        HostApp(pkg, name, true, p.versionName)
    } catch (_: PackageManager.NameNotFoundException) {
        HostApp(pkg, name, false, null)
    }
}
