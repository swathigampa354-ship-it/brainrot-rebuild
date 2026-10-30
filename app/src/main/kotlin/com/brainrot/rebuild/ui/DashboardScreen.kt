package com.brainrot.rebuild.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brainrot.rebuild.app.Increments
import com.brainrot.rebuild.app.RebuildApp
import com.brainrot.rebuild.app.Thresholds
import com.brainrot.rebuild.room.DailyCounterEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    app: RebuildApp,
    thresholds: ThresholdsParams,
    modifier: Modifier = Modifier
) {
    var today by remember { mutableStateOf<DailyCounterEntity?>(null) }
    var recent by remember { mutableStateOf<List<DailyCounterEntity>>(emptyList()) }
    var freshOpen by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        app.db.countersDao().observeLatest().collect { today = it }
    }
    LaunchedEffect(Unit) {
        freshOpen = !(app.thresholds.flow.first().freshStartClosed)
    }
    LaunchedEffect(Unit) {
        val from = LocalDate.now().minusDays(29).toString()
        app.db.countersDao().observeRange(from).collect { recent = it }
    }

    val streak = remember(recent) { streakDays(recent) }
    val milestoneAt = thresholds.reelsLimit * 5
    val milestone = (today?.reelsCount ?: 0) >= milestoneAt && milestoneAt > 0

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Today", style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Metric(today?.reelsCount ?: 0, "reels", Modifier.weight(1f))
            Metric(today?.appOpens ?: 0, "opens", Modifier.weight(1f))
            Metric(today?.phoneUnlocks ?: 0, "unlocks", Modifier.weight(1f))
        }

        // streak mirror of the original milestone experience
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Streak", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (streak > 0) "🔥 $streak day streak of counted reels"
                    else "No streak yet — watch a reel worth counting"
                )
                if (milestone) {
                    Text(
                        "🏆 Milestone crossed: $milestoneAt+ reels — keep the guard on",
                        Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        // Home accuracy upsell (original text parity)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Accuracy", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Reels scrolled on mute aren't counted. Turn on accessibility for exact counting.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Limits", style = MaterialTheme.typography.titleSmall)
                Text(
                    "reels ≤ ${thresholds.reelsLimit ?: 30}, opens ≤ ${thresholds.openLimit ?: 40}, " +
                        "block ${if ((thresholds.blockEnabled ?: true) == true) "on" else "off"}"
                )
            }
        }

        if (freshOpen) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Fresh start", style = MaterialTheme.typography.titleMedium)
                    Text("Your counter starts at zero today. This one-time hint only shows once.")
                    Button(onClick = {
                        freshOpen = false
                        scope.launch { app.thresholds.setFreshStartClosed(true) }
                    }) { Text("Got it") }
                }
            }
        }
    }
}

// streak: consecutive days (from today backwards, until a zero-reel day)
private fun streakDays(rows: List<DailyCounterEntity>): Int {
    val sorted = rows.sortedByDescending { it.dayKey }
    var n = 0
    var cursor = LocalDate.now()
    val byDate = sorted.associateBy { it.dayKey }
    while (true) {
        val row = byDate[cursor.toString()] ?: break
        if (row.reelsCount <= 0) break
        n++
        cursor = cursor.minusDays(1)
    }
    return n
}
