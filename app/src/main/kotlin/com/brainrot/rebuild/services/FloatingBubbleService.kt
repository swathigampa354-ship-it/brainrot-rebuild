package com.brainrot.rebuild.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.brainrot.rebuild.R
import com.brainrot.rebuild.app.RebuildApp
import java.time.LocalDate

/**
 * Brainrot parity feature: a floating bubble on top of every screen showing the
 * rolling reels counter (mirrors AppOpenFloatingService/ReelsCounterFloatingService
 * behavior from the decompiled sources — the original binds a third-party bubble
 * library, this rebuild hand-rolls the WindowManager overlay).
 */
class FloatingBubbleService : Service() {

    private var wm: WindowManager? = null
    private var view: LinearLayout? = null
    private var label: TextView? = null
    private var lastCount = -1
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var stopFlag = false

    private val refresh = object : Runnable {
        override fun run() {
            val n = currentReels()
            if (n != lastCount) {
                lastCount = n
                label?.text = "Reels $n"
            }
            handler.postDelayed(this, REFRESH_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as RebuildApp
        val note = Notification.Builder(this, app.getString(R.string.channel_progress))
            .setContentTitle("Floating counter active")
            .setSmallIcon(R.drawable.ic_stat)
            .setOngoing(true)
            .build()
        startForeground(FG_ID, note)

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (view == null) {
            wm = getSystemService(WINDOW_SERVICE) as? WindowManager
            view = buildView()
            if (view != null) {
                try {
                    wm?.addView(view, layoutParams())
                } catch (_: Exception) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                handler.post(refresh)
            }
        }
        return START_STICKY
    }

    /** Buildings the bubble view: a small draggable 88 dp rounded chip. */
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun buildView(): LinearLayout {
        val ctx = this
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root.setPadding(20, 16, 20, 16)
        root.background = GradientRound()
        val txt = TextView(ctx).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            text = "Reels $lastCount"
        }
        label = txt
        root.addView(
            txt,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        var tmpX = 0f
        var tmpY = 0f
        var initX = 0
        var initY = 0
        var downRawX = 0f
        var downRawY = 0f
        root.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    tmpX = v.x
                    tmpY = v.y
                    initX = tmpX.toInt()
                    initY = tmpY.toInt()
                    downRawX = ev.rawX
                    downRawY = ev.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    v.x = tmpX + (ev.rawX - downRawX)
                    v.y = tmpY + (ev.rawY - downRawY)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    try {
                        val lp = view?.layoutParams as? WindowManager.LayoutParams
                        if (lp != null) {
                            lp.x = v.x.toInt()
                            lp.y = v.y.toInt()
                            wm?.updateViewLayout(view, lp)
                        }
                    } catch (_: Exception) { }
                    true
                }
                else -> false
            }
        }
        return root
    }

    private fun currentReels(): Int =
        try {
            val app = application as RebuildApp
            kotlinx.coroutines.runBlocking {
                app.db.countersDao().dayCounters(LocalDate.now().toString())?.reelsCount ?: 0
            }
        } catch (_: Exception) {
            0
        }

    class GradientRound : android.graphics.drawable.GradientDrawable() {
        init {
            setColor(0xCC101820.toInt())
            cornerRadius = 36f
        }
    }

    private fun layoutParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 20
        y = 220
    }

    override fun onDestroy() {
        stopFlag = true
        handler.removeCallbacks(refresh)
        try {
            view?.let { wm?.removeView(it) }
        } catch (_: Exception) { }
        view = null
        super.onDestroy()
    }

    companion object {
        const val FG_ID = 4042
        const val REFRESH_MS = 800L

        fun ensure(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            try {
                context.startForegroundService(Intent(context, FloatingBubbleService::class.java))
            } catch (_: Exception) { }
        }

        fun stop(context: Context) {
            try { context.stopService(Intent(context, FloatingBubbleService::class.java)) } catch (_: Exception) { }
        }

        fun openOverlaySettings(context: Context) {
            val i = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + context.packageName)
            )
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try { context.startActivity(i) } catch (_: Exception) { }
        }
    }
}
