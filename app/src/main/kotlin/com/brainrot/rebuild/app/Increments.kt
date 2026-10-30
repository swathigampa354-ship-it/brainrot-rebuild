package com.brainrot.rebuild.app

import android.content.Context
import com.brainrot.rebuild.room.DailyCounterEntity
import java.time.LocalDate
import kotlin.concurrent.thread

/** Threaded counter bumps shared by services/workers. */
object Increments {

    private fun bump(context: Context, mutate: (DailyCounterEntity) -> DailyCounterEntity) {
        val app = context.applicationContext as RebuildApp
        thread {
            try {
                val dao = app.db.countersDao()
                val day = LocalDate.now().toString()
                val cur = dao.dayCounters(day) ?: DailyCounterEntity(dayKey = day)
                dao.upsertDay(mutate(cur))
            } catch (_: Exception) { }
        }
    }

    fun reel(context: Context) = bump(context) { it.copy(reelsCount = it.reelsCount + 1) }
    fun open(context: Context) = bump(context) { it.copy(appOpens = it.appOpens + 1) }
    fun unlock(context: Context) = bump(context) { it.copy(phoneUnlocks = it.phoneUnlocks + 1) }
    fun blockHit(context: Context) = bump(context) { it.copy(blockHits = it.blockHits + 1) }
}
