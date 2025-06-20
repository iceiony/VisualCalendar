package com.iceiony.visualcalendar

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.preference.PreferenceManager
import java.util.concurrent.atomic.AtomicBoolean
import androidx.core.content.edit

class CalendarAccessibilityService : AccessibilityService() {
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    public override fun onCreate() {
        super.onCreate()
        PreferenceManager
            .getDefaultSharedPreferences(this)
            .edit {
                putBoolean("service_created", true)
            }
    }

    public override fun onServiceConnected() {
        super.onServiceConnected()
        PreferenceManager
            .getDefaultSharedPreferences(this)
            .edit {
                putBoolean("service_connected", true)
            }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Create and add overlay view
        val inflater = LayoutInflater.from(this)
        val container = FrameLayout(this)
        overlayView = inflater.inflate(R.layout.overlay_layout, container)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP

        windowManager?.addView(container, params)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if(event == null) return

        if(event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (packageName.isBlank()) return

            val activityName = event.className?.toString() ?: ""

            val isVisible = overlayView?.visibility == View.VISIBLE

            if (isHomeScreen(packageName) || isOwnOverlay(packageName, activityName)) {
                if (!isVisible) {
                    overlayView?.visibility = View.VISIBLE
                }
            } else {
                if (isVisible) {
                    // Hide the overlay view when another app is opened
                    overlayView?.visibility = View.GONE
                }
            }
        }
    }

    private fun isOwnOverlay(packageName: String, activityName: String): Boolean {
        return (packageName == "com.iceiony.visualcalendar" && activityName == "android.widget.FrameLayout")
    }

    private fun isHomeScreen(packageName: String): Boolean {
        // Replace with more robust logic if needed
        val homePackages = setOf(
            "com.android.launcher", "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.amazon.tahoe"
        )
        return packageName in homePackages
    }

    override fun onInterrupt() {
        // Handle interruption if needed
    }

    override fun onDestroy() {
        if (overlayView != null && windowManager != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
        super.onDestroy()
    }
}
