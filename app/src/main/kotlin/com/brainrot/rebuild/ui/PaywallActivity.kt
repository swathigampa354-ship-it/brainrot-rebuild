package com.brainrot.rebuild.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brainrot.rebuild.app.RebuildApp

/** brainrot://paywall — mirrors the deep-link surface the original drives from
 *  its floating bubble click actions and the Play-billing RevenueCat flow. */
class PaywallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as RebuildApp
        val limitFlow = app.thresholds.flow
        setContent {
            RebuildTheme {
                val limit = limitFlow.collectAsState(initial = null).value?.reelsLimit
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Go unlimited", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Keep your daily reels guard at ${limit ?: 60} / day — or remove the cap and sync your streaks on your own servers.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = { finish() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Continue with the daily limit")
                        }
                        OutlinedButton(
                            onClick = {
                                // like the original billing flow: user-supplied server, no Google Play billing in this rebuild
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("View upgrade options") }
                    }
                }
            }
        }
    }

    companion object {
        fun open(ctx: android.content.Context) {
            try {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("brainrot://paywall")))
            } catch (_: Exception) { }
        }
    }
}
