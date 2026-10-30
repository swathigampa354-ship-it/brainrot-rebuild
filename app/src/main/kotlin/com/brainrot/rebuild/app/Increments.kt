package com.brainrot.rebuild.app

import android.content.Context
import com.brainrot.rebuild.room.DailyCounterEntity
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking

/** Threaded counter bumps shared by services/workers. */
object Increments {

    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + Dispatchers.IO
    )

    private fun bump(context: Context, mutate: (DailyCounterEntity) -> DailyCounterEntity) {
        val app = context.applicationContext as RebuildApp
        scope.launch {
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
