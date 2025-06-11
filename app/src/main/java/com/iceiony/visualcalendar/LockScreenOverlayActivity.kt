package com.iceiony.visualcalendar

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.KeyguardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import android.os.Handler
import android.os.Looper
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.app.Activity
import android.view.Window
import android.view.LayoutInflater
import android.view.View
import android.view.Gravity
import android.graphics.PixelFormat

class LockScreenOverlayActivity : Activity() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    private val keyguardManager by lazy {
        getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }

    private val keyguardCheckHandler = Handler(Looper.getMainLooper())
    private val keyguardCheckRunnable = object : Runnable {
        override fun run() {
            if (!keyguardManager.isKeyguardLocked) {
                finish()
            } else {
                keyguardCheckHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Don't set a content view — we’re doing our own window
        window.setBackgroundDrawableResource(android.R.color.transparent)
        setShowWhenLocked(true)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showFloatingOverlay()

        keyguardCheckHandler.postDelayed(keyguardCheckRunnable, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        keyguardCheckHandler.removeCallbacks(keyguardCheckRunnable)

        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
    }

    private fun showFloatingOverlay() {
        if (overlayView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        overlayView = LayoutInflater.from(this).inflate(R.layout.floating_overlay_view, null)

        //val params = WindowManager.LayoutParams(
        //    WindowManager.LayoutParams.MATCH_PARENT,
        //    WindowManager.LayoutParams.MATCH_PARENT,
        //    WindowManager.LayoutParams.TYPE_APPLICATION,
        //    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        //            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
        //            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        //            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        //    PixelFormat.TRANSLUCENT
        //)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            ,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

        windowManager?.addView(overlayView, params)
    }
}
