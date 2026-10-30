package com.brainrot.rebuild.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import com.brainrot.rebuild.app.RebuildApp
import com.brainrot.rebuild.app.Increments

/**
 * Full-screen blocker overlay that the accessibility service launches when the
 * daily reels limit is crossed.
 */
class BlockOverlayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Increments.blockHit(this)
        setContent {
            BlockOverlay()
        }
    }

    @Composable
    private fun BlockOverlay() {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Daily limit reached", color = androidx.compose.ui.graphics.Color(0xFFFFC107))
                Text("Take a walk. Your reels stack is waiting for tomorrow.")
                Button(onClick = { finish() }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Got it")
                }
            }
        }
    }
}
