package com.brainrot.rebuild.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.brainrot.rebuild.app.RebuildApp
import com.brainrot.rebuild.app.Thresholds
import com.brainrot.rebuild.services.FallbackUsageStatsService

@Composable
fun SettingsScreen(
    thresholds: Thresholds,
    app: RebuildApp,
    onUpdate: (suspend (RebuildApp.() -> Unit)) -> Unit,
    modifier: Modifier = Modifier
) {
    var reelsLimit by remember(thresholds.reelsLimit) {
        mutableStateOf((thresholds.reelsLimit).toString())
    }
    var openLimit by remember(thresholds.openLimit) {
        mutableStateOf((thresholds.openLimit).toString())
    }
    var baseUrl by remember { mutableStateOf("") }
    var packs by remember(thresholds.watchPackages) { mutableStateOf(thresholds.watchPackages) }
    val ctx = LocalContext.current

    LazyColumn(modifier.fillMaxSize().padding(12.dp)) {
        item {
            Text("Settings", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Card(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Daily limits", style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = reelsLimit,
                            onValueChange = { reelsLimit = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Reels/day") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = openLimit,
                            onValueChange = { openLimit = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Opens/day") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).padding(start = 6.dp)
                        )
                    }
                    Button(
                        onClick = {
                            val r = reelsLimit.toIntOrNull() ?: thresholds.reelsLimit
                            val o = openLimit.toIntOrNull() ?: thresholds.openLimit
                            onUpdate {
                                app.thresholds.setReelsLimit(r)
                                app.thresholds.setOpenLimit(o)
                            }
                        },
                        modifier = Modifier.padding(top = 10.dp)
                    ) { Text("Save limits") }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Block when over limit")
                        }
                        Switch(
                            checked = thresholds.blockEnabled,
                            onCheckedChange = { v ->
                                onUpdate { app.thresholds.setBlockEnabled(v) }
                            }
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Overlay warnings")
                        }
                        Switch(
                            checked = thresholds.overlayWarnings,
                            onCheckedChange = { v ->
                                onUpdate { app.thresholds.setOverlayWarnings(v) }
                            }
                        )
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Usage-stats fallback service")
                            Text("Runs when Accessibility is off (PACKAGE_USAGE_STATS)", style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(
                            checked = thresholds.usageFallback,
                            onCheckedChange = { v ->
                                onUpdate { app.thresholds.setUsageFallback(v) }
                                if (v) FallbackUsageStatsService.ensure(ctx)
                                else FallbackUsageStatsService.stop(ctx)
                            }
                        )
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Watch list (package names, comma-separated)")
                    OutlinedTextField(
                        value = packs,
                        onValueChange = { packs = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Button(
                        onClick = { onUpdate { app.thresholds.setWatchPackages(packs) } },
                        modifier = Modifier.padding(top = 10.dp)
                    ) { Text("Save watch list") }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Column(Modifier.padding(12.dp)) {
                    val overlayOk = android.provider.Settings.canDrawOverlays(app)
                    if (!overlayOk) {
                        Button(
                            onClick = {
                                com.brainrot.rebuild.services.FloatingBubbleService.openOverlaySettings(app)
                            }
                        ) { Text("Grant overlay permission") }
                    } else {
                        Button(
                            onClick = {
                                com.brainrot.rebuild.services.FloatingBubbleService.ensure(app)
                            }
                        ) { Text("Show floating counter") }
                        Button(
                            onClick = {
                                com.brainrot.rebuild.services.FloatingBubbleService.stop(app)
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text("Stop floating counter") }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Cloud sync (your own server)")
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base URL, e.g. https://host/") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Button(
                        onClick = { onUpdate { app.thresholds.setBaseUrl(baseUrl) } },
                        modifier = Modifier.padding(top = 10.dp)
                    ) { Text("Save sync URL") }
                }
            }
        }
    }
}
