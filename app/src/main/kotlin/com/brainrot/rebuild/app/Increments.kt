package com.brainrot.rebuild.app

import android.content.Context
import com.brainrot.rebuild.room.AppSplitEntity
import com.brainrot.rebuild.room.DailyCounterEntity
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Threaded counter bumps shared by services/workers; mirrors the original
 *  per-app split bookkeeping (AppOpenCounter / reelsScrollManager). */
object Increments {

    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + Dispatchers.IO
    )
    private val lock = Mutex()

    private fun bump(context: Context, mutate: (DailyCounterEntity) -> DailyCounterEntity) {
        val app = context.applicationContext as RebuildApp
        scope.launch {
            try {
                lock.withLock {
                    val dao = app.db.countersDao()
                    val day = LocalDate.now().toString()
                    val cur = dao.dayCounters(day) ?: DailyCounterEntity(dayKey = day)
                    dao.upsertDay(mutate(cur))
                }
            } catch (_: Exception) { }
        }
    }

    private fun bumpSplit(context: Context, pkg: String, kind: String) {
        val app = context.applicationContext as RebuildApp
        scope.launch {
            try {
                lock.withLock {
                    val dao = app.db.countersDao()
                    val day = LocalDate.now().toString()
                    val rowKey = day + ":" + pkg
                    val row = dao.splitFor(day).firstOrNull { it.packageName == pkg }
                    dao.upsertSplits(
                        listOf(
                            (row ?: AppSplitEntity(dayKey = day, packageName = pkg, rowKey = rowKey)).let {
                                if (kind == "reel") it.copy(reels = it.reels + 1)
                                else it.copy(opens = it.opens + 1)
                            }
                        )
                    )
                    dao.addEvent(
                        com.brainrot.rebuild.room.RollingEventEntity(
                            ts = System.currentTimeMillis(),
                            kind = kind,
                            packageName = pkg
                        )
                    )
                }
            } catch (_: Exception) { }
        }
    }

    fun reel(context: Context) = bump(context) { it.copy(reelsCount = it.reelsCount + 1) }
    fun reel(context: Context, pkg: String) {
        bump(context) { it.copy(reelsCount = it.reelsCount + 1) }
        bumpSplit(context, pkg, "reel")
    }
    fun open(context: Context) = bump(context) { it.copy(appOpens = it.appOpens + 1) }
    fun open(context: Context, pkg: String) {
        bump(context) { it.copy(appOpens = it.appOpens + 1) }
        bumpSplit(context, pkg, "open")
    }
    fun unlock(context: Context) = bump(context) { it.copy(phoneUnlocks = it.phoneUnlocks + 1) }
    fun blockHit(context: Context) = bump(context) { it.copy(blockHits = it.blockHits + 1) }
}
