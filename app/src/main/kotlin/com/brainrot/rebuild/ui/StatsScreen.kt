package com.brainrot.rebuild.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brainrot.rebuild.app.RebuildApp
import com.brainrot.rebuild.room.AppSplitEntity
import com.brainrot.rebuild.room.DailyCounterEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.first

@Composable
fun StatsScreen(app: RebuildApp, modifier: Modifier = Modifier) {
    var today by remember { mutableStateOf<DailyCounterEntity?>(null) }
    var week by remember { mutableStateOf<List<DailyCounterEntity>>(emptyList()) }
    var split by remember { mutableStateOf<List<AppSplitEntity>>(emptyList()) }
    var limit by remember { mutableStateOf(30) }
    LaunchedEffect(Unit) {
        val dayKey = LocalDate.now().toString()
        app.db.countersDao().observeLatest().collect { today = it }
        app.db.countersDao().observeSplit(dayKey).collect { split = it }
    }
    LaunchedEffect(Unit) {
        app.db.countersDao().observeRange(LocalDate.now().minusDays(6).toString())
            .collect { week = it }
    }
    LaunchedEffect(Unit) {
        limit = app.thresholds.flow.first().reelsLimit
    }
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Stats", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric(today?.reelsCount ?: 0, "reels", Modifier.weight(1f))
                Metric(today?.appOpens ?: 0, "opens", Modifier.weight(1f))
                Metric(today?.blockHits ?: 0, "blocks", Modifier.weight(1f))
            }
        }
        item { Text("Last 7 days", style = MaterialTheme.typography.titleSmall) }
        items(week.filter { it.dayKey >= LocalDate.now().minusDays(6).toString() }, key = { it.dayKey }) { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Row {
                        Text(row.dayKey, Modifier.weight(1f))
                        Text("${row.reelsCount} reels · ${row.appOpens} opens")
                    }
                    LinearProgressIndicator(
                        progress = { row.reelsCount.toFloat() / limit.coerceAtLeast(1).toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp)
                    )
                }
            }
        }
        item { Text("Top packages today", style = MaterialTheme.typography.titleSmall) }
        items(split, key = { it.rowKey }) { row ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(row.packageName, Modifier.weight(1f))
                    Text("opens ${row.opens} · reels ${row.reels}")
                }
            }
        }
    }
}
