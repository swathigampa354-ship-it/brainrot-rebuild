package com.brainrot.rebuild.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.brainrot.rebuild.app.RebuildApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Duel/battle screen (original: brainrot://duel, invite and duel endpoints,
 * duel vote + leaderboard notifications). This rebuild does not ship the
 * vendor's server: join codes and votes go to YOUR server base URL
 * (Settings -> Cloud sync). Without a base URL the surface shows guidance.
 */
@Composable
fun DuelScreen(app: RebuildApp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var baseUrl by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        baseUrl = app.thresholds.baseUrlStr.firstOrNull() ?: ""
    }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Duel a friend", style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Today, your reel count is your duel weapon. Whoever ends up with fewer reels wins the day.")
                if (baseUrl.isBlank()) {
                    Text(
                        "Add your own challenge server base URL under Settings → Cloud sync to host duels.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Invite code (from your server)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { scope.launch { joinDuel(app, code, baseUrl, ctx, { status = it }) } }
                        ) { Text("Join") }
                    }
                }
                status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Invite", style = MaterialTheme.typography.titleSmall)
                Text("Share a duel link with a friend: brainrot://duel", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = {
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND)
                        send.type = "text/plain"
                        send.putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            "Join my daily reels duel! Join code: $code  brainrot://duel"
                        )
                        ctx.startActivity(android.content.Intent.createChooser(send, "Send duel invite"))
                    }
                ) { Text("Share invite") }
            }
        }
    }
}

private suspend fun joinDuel(
    app: RebuildApp,
    code: String,
    baseUrl: String,
    ctx: android.content.Context,
    onResult: (String) -> Unit
) = withContext(Dispatchers.IO) {
    if (code.isBlank()) {
        withContext(Dispatchers.Main) { onResult("Enter an invite code first.") }
        return@withContext
    }
    val url = baseUrl.trimEnd('/') + "/invite/join"
    val payload = "{\"code\":\"" + code.replace("\"", "") + "\"}"
    val req = Request.Builder()
        .url(url.trim())
        .post(payload.toRequestBody("application/json".toMediaTypeOrNull()))
        .build()
    try {
        val client = OkHttpClient()
        client.newCall(req).execute().use { resp ->
            val bodyText = resp.body?.string() ?: ""
            withContext(Dispatchers.Main) {
                onResult("Server: " + resp.code + " " + bodyText.take(80))
            }
        }
    } catch (t: Throwable) {
        withContext(Dispatchers.Main) { onResult("Request failed: " + t.message) }
    }
}
