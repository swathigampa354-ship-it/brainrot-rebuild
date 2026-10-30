package com.brainrot.rebuild.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brainrot.rebuild.services.FallbackUsageStatsService

/**
 * The whole product keys off the a11y service ("BrainPal 🧠" in the original):
 * detect short-form content, count reels, block them. This gate mirrors the
 * original onboarding: link into accessibility settings, ask for notifications
 * (API 33+), and offer usage access as the counter fallback. The app home is
 * only shown once accessibility is enabled.
 */
data class PermissionState(
    val a11y: Boolean,
    val notifications: Boolean,
    val usageAccess: Boolean,
    val installFromPlay: Boolean = true,
    val restrictedLikely: Boolean = false
)

fun checkPermissions(ctx: Context): PermissionState {
    val cls = ctx.packageName + "/" + "com.brainrot.rebuild.services.ReelsAccessibilityService"
    val clsAlt = ctx.packageName + "/.services.ReelsAccessibilityService"
    val enabled = try {
        Settings.Secure.getInt(ctx.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1 &&
            (Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: "")
                .let { it.contains(cls) || it.contains(clsAlt) || it.contains(ctx.packageName, ignoreCase = true) }
    } catch (_: Exception) {
        false
    }
    var fromPlay = true
    try {
        val psi = ctx.packageManager.getInstallSourceInfo(ctx.packageName)
        fromPlay = psi.installingPackageName == "com.android.vending"
    } catch (_: Exception) { fromPlay = true }
    val notif = if (Build.VERSION.SDK_INT >= 33) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true
    val usage = try {
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            "android:get_usage_stats",
            android.os.Process.myUid(), ctx.packageName
        )
        mode == android.app.AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) {
        false
    }
    // Android 13+ restricts accessibility for apps installed outside a store;
    // ColorOS adds its own verification step. We surface it instead of bypassing.
    return PermissionState(
        a11y = enabled,
        notifications = notif,
        usageAccess = usage,
        installFromPlay = fromPlay,
        restrictedLikely = !fromPlay
    )
}

@Composable
fun PermissionGate(
    state: PermissionState,
    onRecheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Set up your counter",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            "BrainPal-style short-form blocking only counts your reels once its watcher is on. Grant each step below.",
            style = MaterialTheme.typography.bodyMedium
        )
        PermStep(
            label = "1. Enable accessibility watcher",
            desc = "Detect short-form content, count reels, and block them. Data never leaves your phone.",
            done = state.a11y
        ) {
            ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        if (state.restrictedLikely && !state.a11y) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Can't tap the toggle? (restricted settings)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "This app was installed outside an app store, so Android " +
                            Build.VERSION.SDK_INT + " keeps this toggle unavailable until you " +
                            "explicitly allow it — the legitimate fix, no security change:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Stock Android: App info (use the button below) → three-dot menu → " +
                            "Allow restricted settings → back here → Recheck.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "OPPO ColorOS: Settings → Apps → App management → Brainrot Rebuild → " +
                            "⋮ → Allow restricted settings (or 'unofficial source' confirmation dialog) → " +
                            "back → Recheck.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            try {
                                ctx.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + ctx.packageName)
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) { }
                        }
                    ) { Text("Open this app's App info") }
                }
            }
        }
        PermStep(
            label = "2. Allow notifications",
            desc = "Keeps the counter service alive in the foreground.",
            done = state.notifications
        ) {
            if (Build.VERSION.SDK_INT >= 33) {
                val act = ctx as? Activity
                act?.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        PermStep(
            label = "3. Usage access (optional fallback)",
            desc = "Estimates reels while accessibility is off. Counts may be approximate.",
            done = state.usageAccess
        ) {
            ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Button(onClick = onRecheck, modifier = Modifier.fillMaxWidth()) { Text("Recheck") }
        if (state.a11y) {
            Text(
                "Watcher is running",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermStep(
    label: String,
    desc: String,
    done: Boolean,
    onAction: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(if (done) "✅" else "•")
            }
            Text(desc, style = MaterialTheme.typography.bodySmall)
            if (!done) {
                Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text("Open") }
            }
        }
    }
}
