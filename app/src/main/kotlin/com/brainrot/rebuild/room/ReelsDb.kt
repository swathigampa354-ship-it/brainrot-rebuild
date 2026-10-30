package com.brainrot.rebuild.room

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "daily_counters")
data class DailyCounterEntity(
    @PrimaryKey val dayKey: String,
    val reelsCount: Int = 0,
    val appOpens: Int = 0,
    val phoneUnlocks: Int = 0,
    val blockHits: Int = 0
)

@Entity(tableName = "app_split")
data class AppSplitEntity(
    @PrimaryKey val rowKey: String,
    val dayKey: String,
    val packageName: String,
    val opens: Int = 0,
    val reels: Int = 0
)

@Entity(tableName = "rolling_events")
data class RollingEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val kind: String,
    val packageName: String
)

@Dao
interface CountersDao {
    @Query("SELECT * FROM daily_counters WHERE dayKey = :dayKey LIMIT 1")
    fun observeDay(dayKey: String): Flow<DailyCounterEntity?>

    @Query("SELECT * FROM daily_counters WHERE dayKey = :dayKey LIMIT 1")
    suspend fun dayCounters(dayKey: String): DailyCounterEntity?

    @Query("SELECT * FROM daily_counters ORDER BY dayKey DESC LIMIT 1")
    fun observeLatest(): Flow<DailyCounterEntity?>

    @Upsert
    suspend fun upsertDay(e: DailyCounterEntity)

    @Query("SELECT * FROM app_split WHERE dayKey = :dayKey ORDER BY opens DESC")
    fun observeSplit(dayKey: String): Flow<List<AppSplitEntity>>

    @Query("SELECT * FROM app_split WHERE dayKey = :dayKey ORDER BY opens DESC")
    suspend fun splitFor(dayKey: String): List<AppSplitEntity>

    @Upsert
    suspend fun upsertSplits(rows: List<AppSplitEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addEvent(e: RollingEventEntity)

    @Query("DELETE FROM rolling_events WHERE ts < :cut")
    suspend fun prune(cut: Long)
}

@Dao
interface UpdatesDao {
    @Query("SELECT COUNT(*) FROM rolling_events WHERE kind = 'reel' AND packageName = :pkg AND ts > :since")
    suspend fun reelsSince(pkg: String, since: Long): Int
}

@Database(
    entities = [DailyCounterEntity::class, AppSplitEntity::class, RollingEventEntity::class],
    version = 1
)
abstract class ReelsDb : RoomDatabase() {
    abstract fun countersDao(): CountersDao
    abstract fun updatesDao(): UpdatesDao
}
