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
import com.ace.toolbox.ui.components.*

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onTools: () -> Unit,
    onScript: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val hosts = remember { AppDetector.detect(context) }
    val installed = hosts.count { it.installed }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        Column(Modifier.padding(horizontal = 26.dp, vertical = 10.dp)) {
            Text(
                "ACE 工具箱",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "QQ / 微信模块能力中心",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DashboardHero(
            title = "模块已就绪",
            subtitle = "Modern LSPosed API 102 · 静态作用域 QQ / 微信",
            badge = "$installed/2 宿主",
            icon = Icons.Rounded.Extension
        )

        Text(
            "功能中心",
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.weight(1f)) {
                FeatureTile(
                    Icons.Rounded.CleaningServices,
                    "工具中心",
                    "分类清理、报告与后续工具",
                    badge = "常用",
                    onClick = onTools
                )
            }
            Box(Modifier.weight(1f)) {
                FeatureTile(
                    Icons.Rounded.Code,
                    "Java 脚本",
                    "自动执行与消息 / 群回调",
                    badge = "实验",
                    onClick = onScript
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.weight(1f)) {
                FeatureTile(
                    Icons.Rounded.Fingerprint,
                    "SSAID",
                    "QQ ANDROID_ID 独立配置",
                    onClick = onSettings
                )
            }
            Box(Modifier.weight(1f)) {
                FeatureTile(
                    Icons.Rounded.Tune,
                    "模块设置",
                    "兼容规则与模块行为",
                    onClick = onSettings
                )
            }
        }

        MiuiSection("目标应用") {
            hosts.forEachIndexed { index, app ->
                MiuiRow(
                    if (app.displayName == "QQ") Icons.Rounded.Chat else Icons.Rounded.ChatBubble,
                    app.displayName,
                    if (app.installed) "版本 ${app.version ?: "?"}" else "未安装",
                    {
                        StatusPill(
                            if (app.installed) "已识别" else "未安装",
                            app.installed
                        )
                    },
                    if (app.installed) {
                        {
                            context.packageManager
                                .getLaunchIntentForPackage(app.packageName)
                                ?.let {
                                    context.startActivity(
                                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                        }
                    } else null
                )

                if (index != hosts.lastIndex) {
                    HorizontalDivider(
                        Modifier.padding(start = 70.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f)
                    )
                }
            }
        }

        Text(
            "后续新增功能优先进入「工具中心」或独立功能页，避免继续堆叠在设置页。",
            Modifier.padding(horizontal = 26.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
