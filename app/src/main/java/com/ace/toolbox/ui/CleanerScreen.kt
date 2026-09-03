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
import com.ace.toolbox.data.ReportStore
import com.ace.toolbox.ui.components.MiuiRow
import com.ace.toolbox.ui.components.MiuiSection

@Composable
fun CleanerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val hosts = remember { AppDetector.detect(context) }
    var reports by remember { mutableStateOf(ReportStore.load(context)) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 20.dp, bottom = 22.dp)
    ) {
        Column(Modifier.padding(horizontal = 26.dp, vertical = 12.dp)) {
            Text(
                "工具中心",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "清理、报告与后续宿主工具统一收纳",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        MiuiSection("清理与存储") {
            MiuiRow(
                Icons.Rounded.LooksOne,
                "安全清理入口",
                "从 QQ / 微信原生设置中的 ACE 工具箱进入分类清理"
            )
            HorizontalDivider(
                Modifier.padding(start = 58.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)
            )
            MiuiRow(
                Icons.Rounded.LooksTwo,
                "分类选择",
                "基础缓存默认勾选，聊天媒体等深度项由用户自行选择"
            )
            HorizontalDivider(
                Modifier.padding(start = 58.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)
            )
            MiuiRow(
                Icons.Rounded.Looks3,
                "二次确认",
                "显示预计释放空间、文件数量后再执行"
            )
        }

        MiuiSection("快速打开") {
            hosts.filter { it.installed }.forEach { app ->
                MiuiRow(
                    Icons.Rounded.OpenInNew,
                    "打开 ${app.displayName}",
                    "版本 ${app.version}",
                    onClick = {
                        context.packageManager
                            .getLaunchIntentForPackage(app.packageName)
                            ?.let {
                                context.startActivity(
                                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                    }
                )
            }
        }

        MiuiSection("保护策略") {
            MiuiRow(
                Icons.Rounded.Storage,
                "数据库保护",
                "跳过 .db / .sqlite / EnMicroMsg.db 等"
            )
            HorizontalDivider(
                Modifier.padding(start = 58.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)
            )
            MiuiRow(
                Icons.Rounded.FolderOff,
                "用户数据保护",
                "跳过 message / contact / favorite / account / auth / wallet 等路径"
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "清理报告",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "由 QQ / 微信宿主进程回传",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = { reports = ReportStore.load(context) }) {
                Text("刷新")
            }
        }

        if (reports.isEmpty()) {
            MiuiSection("暂无记录") {
                MiuiRow(
                    Icons.Rounded.ReceiptLong,
                    "还没有清理报告",
                    "从 QQ / 微信设置里的 ACE 工具箱完成一次清理后会显示在这里"
                )
            }
        } else {
            MiuiSection("最近 ${reports.size} 次") {
                reports.forEachIndexed { i, r ->
                    val name = if (r.packageName == "com.tencent.mobileqq") "QQ" else "微信"
                    MiuiRow(
                        Icons.Rounded.DeleteSweep,
                        "$name · 释放 ${ReportStore.size(r.freed)}",
                        "${ReportStore.time(r.time)} · ${r.deleted} 文件 · ${r.hostVersion}"
                    )
                    if (i != reports.lastIndex) {
                        HorizontalDivider(
                            Modifier.padding(start = 58.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)
                        )
                    }
                }
            }
        }
    }
}
