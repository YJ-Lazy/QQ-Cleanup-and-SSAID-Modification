package com.ace.toolbox.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding

private enum class Page { Home, Cleaner, Reports, Settings }

@Composable
fun AceApp() {
    var page by remember { mutableStateOf(Page.Home) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    Triple(Page.Home, Icons.Rounded.Home, "首页"),
                    Triple(Page.Cleaner, Icons.Rounded.CleaningServices, "清理"),
                    Triple(Page.Reports, Icons.Rounded.ReceiptLong, "报告"),
                    Triple(Page.Settings, Icons.Rounded.Settings, "设置")
                ).forEach { (p, icon, label) ->
                    NavigationBarItem(selected = page == p, onClick = { page = p }, icon = { Icon(icon, null) }, label = { Text(label) })
                }
            }
        }
    ) { pad ->
        when (page) {
            Page.Home -> HomeScreen(Modifier.padding(pad), onCleaner = { page = Page.Cleaner }, onSettings = { page = Page.Settings })
            Page.Cleaner -> CleanerScreen(Modifier.padding(pad))
            Page.Reports -> ReportsScreen(Modifier.padding(pad))
            Page.Settings -> SettingsScreen(Modifier.padding(pad))
        }
    }
}
