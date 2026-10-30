package com.brainrot.rebuild.services

import android.app.Notification
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.brainrot.rebuild.R
import com.brainrot.rebuild.app.Increments
import com.brainrot.rebuild.app.RebuildApp
import kotlinx.coroutines.flow.first
import kotlin.concurrent.thread

class FallbackUsageStatsService : Service() {

    private var ticker: Thread? = null
    @Volatile private var stopFlag = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as RebuildApp
        val note = Notification.Builder(this, app.getString(R.string.channel_progress))
            .setContentTitle("Counting in background (usage-stats fallback)")
            .setSmallIcon(R.drawable.ic_stat)
            .setOngoing(true)
            .build()
        startForeground(FG_ID, note)
        if (ticker?.isAlive != true) {
            stopFlag = false
            ticker = thread(start = true) {
                while (!stopFlag) {
                    try { kotlinx.coroutines.runBlocking { scan(app) } } catch (_: Exception) { }
                    try { Thread.sleep(60_000) } catch (_: InterruptedException) { return@thread }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopFlag = true
        ticker?.interrupt()
        super.onDestroy()
    }

    private suspend fun scan(app: RebuildApp) {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val t = app.thresholds.flow.first()
        val now = System.currentTimeMillis()
        val map = usm.queryEvents(now - 120_000, now)
        val e = UsageEvents.Event()
        val seen = mutableSetOf<String>()
        while (map.hasNextEvent()) {
            map.getNextEvent(e)
            val pkg = e.packageName ?: continue
            if (pkg !in t.watchList()) continue
            if (e.eventType != UsageEvents.Event.ACTIVITY_RESUMED) continue
            if (!seen.add(pkg)) continue
            Increments.open(this)
        }
    }

    companion object {
        const val FG_ID = 4041
        fun ensure(context: Context) {
            try {
                context.startForegroundService(Intent(context, FallbackUsageStatsService::class.java))
            } catch (_: Exception) { }
        }
        fun stop(context: Context) {
            try { context.stopService(Intent(context, FallbackUsageStatsService::class.java)) } catch (_: Exception) { }
        }
    }
}
