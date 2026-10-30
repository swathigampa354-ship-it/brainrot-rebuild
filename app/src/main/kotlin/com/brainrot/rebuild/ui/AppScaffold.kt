package com.brainrot.rebuild.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.brainrot.rebuild.app.RebuildApp

@Composable
fun AppScaffold(
    thresholds: com.brainrot.rebuild.app.Thresholds,
    setThresholds: suspend (suspend com.brainrot.rebuild.app.RebuildApp.() -> Unit) -> Unit,
    tab: Int,
    onTab: (Int) -> Unit,
    requestFallback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appkey = com.brainrot.rebuild.app.RebuildApp.latest
    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    0 to "Home" to Icons.Default.Home,
                    1 to "Stats" to Icons.Default.BarChart,
                    2 to "Settings" to Icons.Default.Settings
                ).forEach { (pair, icon) ->
                    val (i, label) = pair
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { onTab(i) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            val app = appkey
            when (tab) {
                0 -> DashboardScreen(
                    app = app,
                    thresholds = ThresholdsParams(
                        reelsLimit = thresholds.reelsLimit,
                        openLimit = thresholds.openLimit,
                        blockEnabled = thresholds.blockEnabled
                    )
                )
                1 -> StatsScreen(app)
                2 -> SettingsScreen(
                    thresholds = thresholds,
                    app = app,
                    tabs = tab,
                    onTab = onTab,
                    onUpdate = { block -> setThresholds.invoke(block) }
                )
            }
        }
    }
}


