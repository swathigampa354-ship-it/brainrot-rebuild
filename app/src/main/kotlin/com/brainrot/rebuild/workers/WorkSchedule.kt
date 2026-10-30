package com.brainrot.rebuild.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkSchedule {

    const val DAILY_RESET = "daily_reset_workers"
    const val COUNTER_REFRESH = "counter_refresh_workers"
    const val CLOUD_SYNC = "cloud_sync_workers"

    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            DAILY_RESET,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyResetWorker>(1, TimeUnit.HOURS).build()
        )
        wm.enqueueUniquePeriodicWork(
            COUNTER_REFRESH,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CounterRefreshWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .build()
        )
        wm.enqueueUniquePeriodicWork(
            CLOUD_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CloudSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        )
    }
}
