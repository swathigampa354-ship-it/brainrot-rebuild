package com.brainrot.rebuild.services

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.brainrot.rebuild.app.Increments
import com.brainrot.rebuild.app.RebuildApp
import com.brainrot.rebuild.app.Thresholds
import com.brainrot.rebuild.ui.BlockOverlayActivity
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Parity port of the original ReelsAccessibilityService: per-app reel
 *  surfaces identified by resource-id markers (instagram clips_*, youtube
 *  reel_recycler, snapchat spotlight_*), an app-open counter with cooldown +
 *  dedupe reset on screen-off, and the blocking overlay at the daily limit. */
class ReelsAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var cache: Thresholds? = null

    private val reelMarkers = mapOf(
        FEATURE_INSTAGRAM to listOf(
            "clips_media_component",
            "clips_video_container",
            "clips_viewer_view_pager",
            "clips_caption_component"
        ),
        FEATURE_YOUTUBE to listOf(
            "reel_recycler",
            "reel_player_page_content"
        ),
        FEATURE_SNAPCHAT to listOf(
            "spotlight_container",
            "snapchat_spotlight_"
        ),
        FEATURE_TIKTOK to listOf(
            "pagination_container",
            "VerticalViewPager"
        ),
        FEATURE_FACEBOOK to listOf("com.facebook.reels")
    )

    // AppOpenCounter state (mirrors original semantics)
    private var lastOpenPkg = ""
    private var lastOpenTs = 0L
    private val openCooldownMs = 3000L

    private var lastReelTs = 0L
    private val reelCooldownMs = 1200L

    private var blockShownAt = 0L

    override fun onCreate() {
        super.onCreate()
        val app = application as RebuildApp
        scope.launch {
            app.thresholds.flow.collect { cache = it }
        }
        try {
            val reset = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    lastOpenPkg = ""
                    lastOpenTs = 0
                }
            }
            registerReceiver(reset, IntentFilter(Intent.ACTION_SCREEN_OFF))
            registerReceiver(reset, IntentFilter(Intent.ACTION_SCREEN_ON))
            registerReceiver(reset, IntentFilter(Intent.ACTION_USER_PRESENT))
        } catch (_: Exception) { }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    private fun isReelSurface(pkg: String, event: AccessibilityEvent): Boolean {
        val markers = reelMarkers[pkg] ?: return false
        val root = rootInActiveWindow ?: return false
        if (root.packageName?.toString() != pkg) return false
        return containsReelMarker(root, markers)
    }

    private fun containsReelMarker(node: AccessibilityNodeInfo, markers: List<String>): Boolean {
        val rid = node.viewIdResourceName ?: ""
        if (markers.any(rid::contains)) return true
        for (i in 0 until node.childCount) {
            val c = node.getChild(i) ?: continue
            if (containsReelMarker(c, markers)) return true
        }
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        val pkg = e.packageName?.toString() ?: return
        val t = cache ?: return
        if (pkg !in t.watchList()) return

        val now = System.currentTimeMillis()

        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (pkg != lastOpenPkg && now - lastOpenTs > openCooldownMs) {
                    lastOpenPkg = pkg
                    lastOpenTs = now
                    Increments.open(this, pkg)
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (now - lastReelTs < reelCooldownMs) return
                lastReelTs = now
                if (isReelSurface(pkg, e)) {
                    Increments.reel(this, pkg)
                }
            }
        }
        maybeBlock(t, pkg)
    }

    private fun maybeBlock(t: Thresholds, pkg: String) {
        if (!t.blockEnabled) return
        val svc = this
        val today = LocalDate.now().toString()
        scope.launch {
            val count = try {
                (svc.application as RebuildApp).db.countersDao().dayCounters(today)?.reelsCount ?: 0
            } catch (_: Exception) { 0 }
            if (t.reelsLimit > 0 && count >= t.reelsLimit &&
                System.currentTimeMillis() - blockShownAt > 15000
            ) {
                blockShownAt = System.currentTimeMillis()
                Increments.blockHit(svc)
                try {
                    svc.startActivity(
                        Intent(svc, BlockOverlayActivity::class.java)
                            .addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            )
                    )
                } catch (_: Exception) { }
            }
        }
    }
}

private const val FEATURE_INSTAGRAM = "com.instagram.android"
private const val FEATURE_YOUTUBE = "com.google.android.youtube"
private const val FEATURE_SNAPCHAT = "com.snapchat.android"
private const val FEATURE_TIKTOK = "com.ss.android.ugc.trill"
private const val FEATURE_FACEBOOK = "com.facebook.katana"
