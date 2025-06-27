package com.iceiony.visualcalendar

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout

class CalendarAccessibilityService : AccessibilityService() {
    private var calendarView: View? = null
    private var windowManager: WindowManager? = null

    private var homePackages = setOf(
        "com.android.launcher", "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",
        "com.amazon.tahoe", // Fire OS launcher package
    )

    public override fun onServiceConnected() {
        super.onServiceConnected()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Create and add overlay view
        val inflater = LayoutInflater.from(this)
        val container = FrameLayout(this)
        calendarView = inflater.inflate(R.layout.overlay_layout, container)
        calendarView?.visibility = View.GONE

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

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val launcherPackageName = resolveInfo?.activityInfo?.packageName
        if (launcherPackageName != null) {
            homePackages = homePackages + launcherPackageName
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if(event == null) return

        if(event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (packageName.isBlank()) return

            val activityName = event.className?.toString() ?: ""

            val isVisible = calendarView?.visibility == View.VISIBLE

            if (isHomeScreen(packageName) || IsOwnOverlay(packageName, activityName)) {
                if (!isVisible) {
                    calendarView?.visibility = View.VISIBLE
                }
            } else {
                if (isVisible) {
                    // Hide the overlay view when another app is opened
                    calendarView?.visibility = View.GONE
                }
            }
        }
    }

    private fun IsOwnOverlay(packageName: String, activityName: String): Boolean {
        return (packageName == "com.iceiony.visualcalendar" && activityName == "android.widget.FrameLayout")
    }

    private fun isHomeScreen(packageName: String): Boolean {
        // Replace with more robust logic if needed
        return packageName in homePackages
    }

    override fun onInterrupt() {
        // Handle interruption if needed
    }

    override fun onDestroy() {
        if (calendarView != null && windowManager != null) {
            windowManager?.removeView(calendarView)
            calendarView = null
        }
        super.onDestroy()
    }
}
