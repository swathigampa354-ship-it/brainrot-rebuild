package com.brainrot.rebuild.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ThresholdsParams(
    val reelsLimit: Int = 30,
    val openLimit: Int = 40,
    val blockEnabled: Boolean = true,
    val overlayWarnings: Boolean = true
)

@Composable
fun Metric(v: Int, label: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(
            modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$v", style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
