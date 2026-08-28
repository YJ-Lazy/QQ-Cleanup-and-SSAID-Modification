package com.ace.toolbox.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ace.toolbox.data.ReportStore
import com.ace.toolbox.ui.components.MiuiRow
import com.ace.toolbox.ui.components.MiuiSection

@Composable
fun ReportsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var reports by remember { mutableStateOf(ReportStore.load(context)) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 20.dp, bottom = 22.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("清理报告", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("由 QQ / 微信宿主进程回传", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { reports = ReportStore.load(context) }) { Text("刷新") }
        }
        if (reports.isEmpty()) {
            MiuiSection("暂无记录") {
                MiuiRow(Icons.Rounded.DeleteSweep, "还没有清理报告", "从 QQ / 微信设置里的 ACE 工具箱完成一次清理后会显示在这里")
            }
        } else {
            MiuiSection("最近 ${reports.size} 次") {
                reports.forEachIndexed { i, r ->
                    val name = if (r.packageName == "com.tencent.mobileqq") "QQ" else "微信"
                    MiuiRow(Icons.Rounded.DeleteSweep, "$name · 释放 ${ReportStore.size(r.freed)}", "${ReportStore.time(r.time)} · ${r.deleted} 文件 · ${r.hostVersion}")
                    if (i != reports.lastIndex) HorizontalDivider(Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=.45f))
                }
            }
        }
    }
}
