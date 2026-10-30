package com.brainrot.rebuild.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.brainrot.rebuild.app.RebuildApp
import java.time.LocalDate

class DailyResetWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as RebuildApp
        return try {
            val today = LocalDate.now().toString()
            val dao = app.db.countersDao()
            if (dao.dayCounters(today) == null) dao.upsertDay(
                com.brainrot.rebuild.room.DailyCounterEntity(dayKey = today)
            )
            dao.prune(System.currentTimeMillis() - 60L * 24 * 3600 * 1000)
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
}
