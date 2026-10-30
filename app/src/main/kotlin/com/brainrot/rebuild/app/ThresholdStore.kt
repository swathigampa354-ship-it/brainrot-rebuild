package com.brainrot.rebuild.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.thrDataStore by preferencesDataStore("brainrot_settings")

data class Thresholds(
    val reelsLimit: Int = 30,
    val openLimit: Int = 40,
    val blockEnabled: Boolean = true,
    val overlayWarnings: Boolean = true,
    val usageFallback: Boolean = true,
    val watchPackages: String =
        "com.instagram.android,com.google.android.youtube,com.reddit.frontpage,com.zhiliaoapp.musically",
    val lastSync: Long = 0,
    val eventNotifications: Boolean = true,
    val tryBrReturn: Boolean = false,
    val freshStartClosed: Boolean = false
) {
    fun watchList(): List<String> = watchPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

class ThresholdStore(private val context: Context) {
    private object K {
        val REELS = intPreferencesKey("reels_limit")
        val OPENS = intPreferencesKey("open_limit")
        val BLOCK = booleanPreferencesKey("block_enabled")
        val OVERLAY = booleanPreferencesKey("overlay_warnings")
        val FALLBACK = booleanPreferencesKey("usage_fallback")
        val PACKS = stringPreferencesKey("watch_packages")
        val BASEURL = stringPreferencesKey("api_base_url")
        val LASTSYNC = longPreferencesKey("last_sync")
        val EVNOTIF = booleanPreferencesKey("event_notifications")
        val TRYBR = booleanPreferencesKey("try_br_return")
        val FRESH = booleanPreferencesKey("fresh_start_closed")
    }

    val flow: Flow<Thresholds> = context.thrDataStore.data.map { p ->
        Thresholds(
            reelsLimit = p[K.REELS] ?: 30,
            openLimit = p[K.OPENS] ?: 40,
            blockEnabled = p[K.BLOCK] ?: true,
            overlayWarnings = p[K.OVERLAY] ?: true,
            usageFallback = p[K.FALLBACK] ?: true,
            watchPackages = p[K.PACKS]
                ?: "com.instagram.android,com.google.android.youtube,com.reddit.frontpage,com.zhiliaoapp.musically",
            lastSync = p[K.LASTSYNC] ?: 0L,
            eventNotifications = p[K.EVNOTIF] ?: true,
            tryBrReturn = p[K.TRYBR] ?: false,
            freshStartClosed = p[K.FRESH] ?: false
        )
    }

    val baseUrlStr: Flow<String> = context.thrDataStore.data.map { p -> p[K.BASEURL] ?: "" }
    suspend fun setBaseUrl(v: String) = context.thrDataStore.edit { it[K.BASEURL] = v.trim() }
    suspend fun setLastSync(v: Long) = context.thrDataStore.edit { it[K.LASTSYNC] = v }

    suspend fun setReelsLimit(v: Int) = context.thrDataStore.edit { it[K.REELS] = v }
    suspend fun setOpenLimit(v: Int) = context.thrDataStore.edit { it[K.OPENS] = v }
    suspend fun setBlockEnabled(v: Boolean) = context.thrDataStore.edit { it[K.BLOCK] = v }
    suspend fun setOverlayWarnings(v: Boolean) = context.thrDataStore.edit { it[K.OVERLAY] = v }
    suspend fun setUsageFallback(v: Boolean) = context.thrDataStore.edit { it[K.FALLBACK] = v }
    suspend fun setWatchPackages(v: String) = context.thrDataStore.edit { it[K.PACKS] = v }
    suspend fun setEventNotifications(v: Boolean) = context.thrDataStore.edit { it[K.EVNOTIF] = v }
    suspend fun setTryBrReturn(v: Boolean) = context.thrDataStore.edit { it[K.TRYBR] = v }
    suspend fun setFreshStartClosed(v: Boolean) = context.thrDataStore.edit { it[K.FRESH] = v }

    fun todayKey(): String = LocalDate.now().toString()
}
