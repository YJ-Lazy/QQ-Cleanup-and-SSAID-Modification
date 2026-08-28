package com.ace.toolbox.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ace.toolbox.data.AppDetector
import com.ace.toolbox.ui.components.MiuiRow
import com.ace.toolbox.ui.components.MiuiSection

@Composable
fun CleanerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val hosts = remember { AppDetector.detect(context) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 20.dp, bottom = 22.dp)) {
        Column(Modifier.padding(horizontal = 26.dp, vertical = 12.dp)) {
            Text("安全清理", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("在宿主进程中执行，避免 Android 11+ 跨应用存储权限问题", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MiuiSection("如何使用") {
            MiuiRow(Icons.Rounded.LooksOne, "启用模块", "在 LSPosed 中勾选 QQ / 微信，并强制停止后重新打开")
            HorizontalDivider(Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=.45f))
            MiuiRow(Icons.Rounded.LooksTwo, "进入宿主设置", "QQ 设置或微信设置中找到「ACE 工具箱」")
            HorizontalDivider(Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=.45f))
            MiuiRow(Icons.Rounded.Looks3, "扫描并确认", "先显示大小、文件数和识别用户目录，再二次确认清理")
        }
        MiuiSection("快速打开") {
            hosts.filter { it.installed }.forEach { app ->
                MiuiRow(Icons.Rounded.OpenInNew, "打开 ${app.displayName}", "版本 ${app.version}", onClick = {
                    context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                })
            }
        }
        MiuiSection("保护策略") {
            MiuiRow(Icons.Rounded.Storage, "数据库保护", "跳过 .db / .sqlite / EnMicroMsg.db 等")
            HorizontalDivider(Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=.45f))
            MiuiRow(Icons.Rounded.FolderOff, "用户数据保护", "跳过 message / contact / favorite / account / auth 路径")
        }
    }
}
