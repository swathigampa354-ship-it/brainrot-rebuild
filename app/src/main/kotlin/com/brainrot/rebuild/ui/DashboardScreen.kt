package com.brainrot.rebuild.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.brainrot.rebuild.room.DailyCounterEntity

@Composable
fun DashboardScreen(
    app: RebuildApp,
    thresholds: ThresholdsParams,
    modifier: Modifier = Modifier
) {
    var today by remember { mutableStateOf<DailyCounterEntity?>(null) }
    LaunchedEffect(Unit) {
        app.db.countersDao().observeLatest().collect { today = it }
    }
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Today", style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Metric(today?.reelsCount ?: 0, "reels", Modifier.weight(1f))
            Metric(today?.appOpens ?: 0, "opens", Modifier.weight(1f))
            Metric(today?.phoneUnlocks ?: 0, "unlocks", Modifier.weight(1f))
        }
        Card(Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Limits", style = MaterialTheme.typography.titleSmall)
                Text(
                    "reels ≤ ${thresholds.reelsLimit ?: 30}, opens ≤ ${thresholds.openLimit ?: 40}, " +
                        "block ${if ((thresholds.blockEnabled ?: true) == true) "on" else "off"}"
                )
            }
        }
    }
}

