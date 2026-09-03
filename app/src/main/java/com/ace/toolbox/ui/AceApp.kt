package com.ace.toolbox.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

private enum class Page { Home, Tools, JavaScript, Settings }

@Composable
fun AceApp() {
    var page by remember { mutableStateOf(Page.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    Triple(Page.Home, Icons.Rounded.Home, "首页"),
                    Triple(Page.Tools, Icons.Rounded.Widgets, "工具"),
                    Triple(Page.JavaScript, Icons.Rounded.Code, "脚本"),
                    Triple(Page.Settings, Icons.Rounded.Settings, "设置")
                ).forEach { (p, icon, label) ->
                    NavigationBarItem(
                        selected = page == p,
                        onClick = { page = p },
                        icon = { Icon(icon, null) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { pad ->
        when (page) {
            Page.Home -> HomeScreen(
                Modifier.padding(pad),
                onTools = { page = Page.Tools },
                onScript = { page = Page.JavaScript },
                onSettings = { page = Page.Settings }
            )
            Page.Tools -> CleanerScreen(Modifier.padding(pad))
            Page.JavaScript -> JavaScriptScreen(Modifier.padding(pad))
            Page.Settings -> SettingsScreen(Modifier.padding(pad))
        }
    }
}
