package com.brainrot.rebuild.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.brainrot.rebuild.app.RebuildApp
import com.brainrot.rebuild.app.Thresholds
import com.brainrot.rebuild.ui.BlockOverlayActivity
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ReelsAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var cache: Thresholds? = null
    private var lastPkg = ""
    private var lastTs = 0L
    private var blockShownAt = 0L

    override fun onCreate() {
        super.onCreate()
        val app = application as RebuildApp
        scope.launch {
            app.thresholds.flow.collect { cache = it }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        val pkg = e.packageName?.toString() ?: return
        val t = cache ?: return
        if (pkg !in t.watchList()) return

        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val now = System.currentTimeMillis()
                if (pkg == lastPkg && now - lastTs < 1500) return
                lastPkg = pkg
                lastTs = now
                Increments.reel(this, pkg)
            }
        }

        val today = LocalDate.now().toString()
        if (!t.blockEnabled) return
        val svc = this
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

    override fun onInterrupt() = Unit
}
