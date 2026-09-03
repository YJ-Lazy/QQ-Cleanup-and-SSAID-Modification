package com.ace.toolbox.ui

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
import com.ace.toolbox.data.AppConfig
import com.ace.toolbox.ui.components.MiuiRow
import com.ace.toolbox.ui.components.MiuiSection

@Composable
fun JavaScriptScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val config = remember { AppConfig(context) }

    var enabled by remember { mutableStateOf(config.javaScriptEnabled) }
    var source by remember { mutableStateOf(config.javaScriptSource) }
    var autoRun by remember { mutableStateOf(config.javaScriptAutoRun) }
    var receiveCallback by remember { mutableStateOf(config.javaScriptReceiveCallback) }
    var sendCallback by remember { mutableStateOf(config.javaScriptSendCallback) }
    var groupCallback by remember { mutableStateOf(config.javaScriptGroupCallback) }
    var showEditor by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 20.dp, bottom = 22.dp)
    ) {
        Column(Modifier.padding(horizontal = 26.dp, vertical = 12.dp)) {
            Text(
                "Java 脚本",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "脚本运行时 · 自动执行 · 消息与群事件回调",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        MiuiSection("运行") {
            MiuiRow(
                Icons.Rounded.Code,
                "启用 Java 脚本",
                if (enabled) "已启用" else "默认关闭",
                {
                    Switch(
                        enabled,
                        {
                            enabled = it
                            config.javaScriptEnabled = it
                        }
                    )
                }
            )
            HorizontalDivider(
                Modifier.padding(start = 58.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)
            )
            MiuiRow(
                Icons.Rounded.Edit,
                "编辑 main.java",
                "${source.length} 字符 · 最大 32 KiB",
                onClick = { showEditor = true }
            )
            HorizontalDivider(
                Modifier.padding(start = 58.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)
            )
            MiuiRow(
                Icons.Rounded.PlayCircle,
                "随 QQ 自动执行",
                if (autoRun) {
                    "QQ 进程启动后首次界面恢复时自动运行一次"
                } else {
                    "关闭时仍可在 QQ 的 ACE 工具箱中手动运行"
                },
                {
                    Switch(
                        autoRun,
                        {
                            autoRun = it
                            config.javaScriptAutoRun = it
                        },
                        enabled = enabled
                    )
                }
            )
        }

        MiuiSection("事件回调") {
            MiuiRow(
                Icons.Rounded.MarkChatUnread,
                "接收消息回调",
                "onMsg(Object msgData)",
                {
                    Switch(
                        receiveCallback,
                        {
                            receiveCallback = it
                            config.javaScriptReceiveCallback = it
                        },
                        enabled = enabled
                    )
                }
            )
            HorizontalDivider(
                Modifier.padding(start = 58.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)
            )
            MiuiRow(
                Icons.Rounded.Send,
                "发送消息回调",
                "onSendMsg(Object msgData)，兼容 onSend(Object)",
                {
                    Switch(
                        sendCallback,
                        {
                            sendCallback = it
                            config.javaScriptSendCallback = it
                        },
                        enabled = enabled
                    )
                }
            )
            HorizontalDivider(
                Modifier.padding(start = 58.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)
            )
            MiuiRow(
                Icons.Rounded.Groups,
                "群事件回调",
                "onGroupEvent；兼容 joinGroup / quitGroup / shutUpGroup",
                {
                    Switch(
                        groupCallback,
                        {
                            groupCallback = it
                            config.javaScriptGroupCallback = it
                        },
                        enabled = enabled
                    )
                }
            )
        }

        MiuiSection("脚本 API") {
            MiuiRow(
                Icons.Rounded.Terminal,
                "宿主变量",
                "ace / context / classLoader / packageName / hostVersion / ssaid"
            )
            HorizontalDivider(
                Modifier.padding(start = 58.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)
            )
            MiuiRow(
                Icons.Rounded.Info,
                "回调线程",
                "事件进入独立有界单线程队列，不直接阻塞 QQ Hook 线程"
            )
            HorizontalDivider(
                Modifier.padding(start = 58.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)
            )
            MiuiRow(
                Icons.Rounded.Security,
                "安全提示",
                "脚本运行在 QQ 进程中，只运行自己编写或审计过的代码"
            )
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text("编辑 main.java") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "保存后可在 QQ → ACE 工具箱手动运行；开启自动执行后，每个 QQ 进程自动加载一次。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = source,
                        onValueChange = { source = it.take(32768) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 300.dp, max = 480.dp),
                        label = { Text("BeanShell / Java-like") },
                        supportingText = { Text("${source.length} / 32768") },
                        minLines = 12,
                        maxLines = 20
                    )
                    OutlinedButton(
                        onClick = { source = AppConfig.DEFAULT_JAVA_SCRIPT },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("恢复回调示例")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        config.javaScriptSource = source
                        showEditor = false
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) { Text("取消") }
            }
        )
    }
}
