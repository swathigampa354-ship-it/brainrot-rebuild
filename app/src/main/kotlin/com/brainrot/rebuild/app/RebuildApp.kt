package com.brainrot.rebuild.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.room.Room
import androidx.work.Configuration
import com.brainrot.rebuild.net.SyncManager
import com.brainrot.rebuild.room.ReelsDb
import com.brainrot.rebuild.app.Increments
import com.brainrot.rebuild.workers.WorkSchedule

class RebuildApp : Application(), Configuration.Provider {

    lateinit var db: ReelsDb
        private set
    lateinit var thresholds: ThresholdStore
        private set
    lateinit var sync: SyncManager
        private set

    companion object {
        @Volatile
        var latest: RebuildApp? = null
    }

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, ReelsDb::class.java, "reels_counts.db")
            .fallbackToDestructiveMigration()
            .build()
        thresholds = ThresholdStore(this)
        sync = SyncManager(this, db, thresholds)
        latest = this
        createChannels()
        registerUnlockCounter()
        WorkSchedule.schedule(this)
    }

    private fun createChannels() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            "brainrot_progress" to "Counting progress",
            "brainrot_block" to "Blocking alerts"
        ).forEach { (id, name) ->
            nm.createNotificationChannel(
                NotificationChannel(
                    id, name,
                    if (id == "brainrot_progress") NotificationManager.IMPORTANCE_LOW
                    else NotificationManager.IMPORTANCE_HIGH
                )
            )
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    /** Phone unlock counting parity (SCREEN_ON / USER_PRESENT in the original). */
    private fun registerUnlockCounter() {
        val recv = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                val act = intent?.action ?: return
                if (act == android.content.Intent.ACTION_USER_PRESENT) {
                    Increments.unlock(applicationContext)
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_USER_PRESENT)
            addAction(android.content.Intent.ACTION_SCREEN_OFF)
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                registerReceiver(recv, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(recv, filter)
            }
        } catch (_: Exception) { }
    }
}
