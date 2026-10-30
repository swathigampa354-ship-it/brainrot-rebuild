package com.brainrot.rebuild.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.brainrot.rebuild.app.RebuildApp
import com.brainrot.rebuild.widgets.ReelsCounterWidget

class CounterRefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = try {
        ReelsCounterWidget().update(applicationContext)
        Result.success()
    } catch (t: Throwable) {
        Result.retry()
    }
}
