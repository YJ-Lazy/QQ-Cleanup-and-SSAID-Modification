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
import com.ace.toolbox.ui.components.StatusPill

@Composable
fun HomeScreen(modifier: Modifier = Modifier, onCleaner: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val hosts = remember { AppDetector.detect(context) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 18.dp, bottom = 22.dp)) {
        Column(Modifier.padding(horizontal = 26.dp, vertical = 16.dp)) {
            Text("ACE 工具箱", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("QQ / 微信清理与增强", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MiuiSection("模块状态") {
            MiuiRow(Icons.Rounded.Extension, "Modern LSPosed API 102", "静态作用域：QQ、微信", { StatusPill("API 102", true) })
            HorizontalDivider(Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=.45f))
            MiuiRow(Icons.Rounded.Security, "安全清理", "仅 cache / tmp / log，数据库和消息目录强制保护", { StatusPill("默认开启", true) })
        }
        MiuiSection("目标应用") {
            hosts.forEachIndexed { index, app ->
                MiuiRow(if (app.displayName == "QQ") Icons.Rounded.Chat else Icons.Rounded.ChatBubble, app.displayName,
                    if (app.installed) "版本 ${app.version ?: "?"}" else "未安装",
                    { StatusPill(if (app.installed) "已识别" else "未安装", app.installed) },
                    if (app.installed) { {
                        context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    } } else null)
                if (index == 0) HorizontalDivider(Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=.45f))
            }
        }
        MiuiSection("快捷功能") {
            MiuiRow(Icons.Rounded.CleaningServices, "缓存清理", "从 QQ / 微信原生设置中的 ACE 工具箱执行", onClick = onCleaner)
            HorizontalDivider(Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=.45f))
            MiuiRow(Icons.Rounded.Rule, "Hook 规则", "内置规则 + 版本化远程规则", onClick = onSettings)
        }
        Text("参考 FunBox 的宿主注入 / 功能分组思路重新实现，不包含其私有源码或 native 库。", Modifier.padding(horizontal = 26.dp, vertical = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
