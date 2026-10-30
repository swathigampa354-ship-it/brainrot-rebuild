package com.brainrot.rebuild.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.brainrot.rebuild.app.RebuildApp
import com.brainrot.rebuild.net.SyncManager

class CloudSyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = (applicationContext as RebuildApp)
        return try {
            app.sync.pushIfConfigured()
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
}
