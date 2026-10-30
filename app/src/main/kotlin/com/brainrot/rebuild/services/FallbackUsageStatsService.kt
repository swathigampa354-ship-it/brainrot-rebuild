package com.brainrot.rebuild.services

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.brainrot.rebuild.R
import com.brainrot.rebuild.app.Increments
import com.brainrot.rebuild.app.RebuildApp
import java.time.LocalDate
import kotlin.concurrent.thread

/**
 * Fallback counter when the accessibility service is off:
 * periodically inspects UsageStatsManager per the pre-declared PACKAGE_USAGE_STATS
 * permission and tops up "app opens" counters for the watch list.
 */
class FallbackUsageStatsService : Service() {

    private var ticker: Thread? = null
    private var volatileStop = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val asApp = application as RebuildApp
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val note = Notification.Builder(this, asApp.getString(R.string.channel_progress))
            .setContentTitle("Counting in background (usage-stats fallback)")
            .setSmallIcon(R.drawable.ic_stat)
            .setOngoing(true)
            .build()
        startForeground(FG_ID, note)

        if (ticker?.isAlive != true) {
            volatileStop = false
            ticker = thread(start = true) {
                while (!volatileStop) {
                    try { scan(asApp) } catch (_: Exception) { }
                    try { Thread.sleep(60_000) } catch (_: InterruptedException) { return@thread }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        volatileStop = true
        ticker?.interrupt()
        super.onDestroy()
    }

    private fun scan(app: RebuildApp) {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val t = kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.flow.first(app.thresholds.flow)
        }
        val now = System.currentTimeMillis()
        val from = now - 120_000
        val map = usm.queryEvents(from, now)
        val e = android.app.usage.UsageEvents.Event()
        val today = LocalDate.now().toString()
        val seen = mutableSetOf<String>()
        while (map.hasNextEvent()) {
            map.getNextEvent(e)
            val pkg = e.packageName ?: continue
            if (pkg !in t.watchList()) continue
            if (e.eventType != android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) continue
            if (!seen.add(pkg)) continue
            Increments.open(this, pkg)
        }
        // top-up manifest-driven reels approximations: count resumed foreground time in watch list
    }

    companion object {
        const val FG_ID = 4041
        fun ensure(context: Context) {
            context.startForegroundService(Intent(context, FallbackUsageStatsService::class.java))
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, FallbackUsageStatsService::class.java))
        }
    }
}
