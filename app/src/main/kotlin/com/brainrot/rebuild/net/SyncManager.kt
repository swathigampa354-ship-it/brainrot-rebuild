package com.brainrot.rebuild.net

import android.content.Context
import com.brainrot.rebuild.app.RebuildApp
import com.brainrot.rebuild.app.ThresholdStore
import com.brainrot.rebuild.room.AppSplitEntity
import com.brainrot.rebuild.room.DailyCounterEntity
import com.brainrot.rebuild.workers.CloudSyncWorker
import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET

data class ConfigDto(
    @SerializedName("reels_limit") val reelsLimit: Int? = null,
    @SerializedName("open_limit") val openLimit: Int? = null,
    @SerializedName("watch_packages") val watchPackages: String? = null
)

data class SyncOutDto(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("since_config") val sinceConfig: Long,
    @SerializedName("daily") val daily: Map<String, DailyRow>,
    @SerializedName("app_splits") val appSplits: List<SplitRow>
)

data class DailyRow(
    @SerializedName("reels") val reels: Int,
    @SerializedName("opens") val opens: Int,
    @SerializedName("unlocks") val unlocks: Int,
    @SerializedName("blocks") val blocks: Int
)

data class SplitRow(
    @SerializedName("date") val date: String,
    @SerializedName("package") val pkg: String,
    @SerializedName("opens") val opens: Int,
    @SerializedName("reels") val reels: Int
)

interface BrainRotApi {
    @GET("/stats/api/v1/user/config")
    suspend fun userConfig(): ConfigDto

    @POST("/stats/api/v1/user/sync")
    suspend fun userSync(@Body body: SyncOutDto): ConfigDto
}

class SyncManager(
    private val context: Context,
    private val db: com.brainrot.rebuild.room.ReelsDb,
    private val thresholds: ThresholdStore
) {
    private val http = OkHttpClient.Builder().build()
    private var baseUrl: String = ""

    fun baseUrlFromPrefs(): String = kotlinx.coroutines.runBlocking {
        thresholds.baseUrlStr.first()
    }

    private fun api(): BrainRotApi? {
        val url = baseUrl.ifEmpty { baseUrlFromPrefs() }
        if (url.isBlank()) return null
        baseUrl = url
        val fixed = if (url.endsWith("/")) url else "$url/"
        return Retrofit.Builder()
            .baseUrl(fixed)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BrainRotApi::class.java)
    }

    suspend fun pullConfig() {
        val api = api() ?: return
        val cfg = api.userConfig()
        if (cfg.reelsLimit != null && cfg.reelsLimit > 0) thresholds.setReelsLimit(cfg.reelsLimit)
        if (cfg.openLimit != null && cfg.openLimit > 0) thresholds.setOpenLimit(cfg.openLimit)
        cfg.watchPackages?.let { if (it.isNotBlank()) thresholds.setWatchPackages(it) }
        thresholds.setLastSync(now())
    }

    suspend fun pushIfConfigured() {
        val api = api() ?: return
        val dao = db.countersDao()
        val today = LocalDate.now().toString()
        val days = mapOf(
            today to (dao.dayCounters(today) ?: DailyCounterEntity(dayKey = today))
        ).mapValues { (k, v) ->
            DailyRow(v.reelsCount, v.appOpens, v.phoneUnlocks, v.blockHits)
        }
        val splits = dao.splitFor(today).map {
            SplitRow(it.dayKey, it.packageName, it.opens, it.reels)
        }
        api.userSync(SyncOutDto(deviceIdOf(), 0L, days, splits))
        thresholds.setLastSync(now())
    }

    private fun deviceIdOf(): String =
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ).orEmpty().ifEmpty { "anon" }

    private fun now() = System.currentTimeMillis() / 1000L
}
