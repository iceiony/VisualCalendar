package com.iceiony.visualcalendar

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.util.Log
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class CalendarAccessibilityService :
    AccessibilityService(),
    androidx.lifecycle.LifecycleOwner,
    androidx.savedstate.SavedStateRegistryOwner
{

    private var calendarView: ComposeView? = null
    private var windowManager: WindowManager? = null
    private var homePackages = setOf(
        "com.android.launcher", "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",
        "com.amazon.tahoe", // Fire OS launcher package
    )

    private var savedStateBundle: Bundle? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(savedStateBundle)

        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    public override fun onServiceConnected() {
        super.onServiceConnected()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        calendarView = ComposeView(this).apply{
            setViewTreeSavedStateRegistryOwner(this@CalendarAccessibilityService)
            setViewTreeLifecycleOwner(this@CalendarAccessibilityService)
            setContent { CalendarDayView() }
        }

        calendarView?.visibility = View.GONE

        val params = WindowManager.LayoutParams(
            550.dpToPx(this),
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP

        windowManager?.addView(calendarView, params)

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val launcherPackageName = resolveInfo?.activityInfo?.packageName
        if (launcherPackageName != null) {
            homePackages = homePackages + launcherPackageName
        }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
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
                if(isVisible) {
                    calendarView?.visibility = View.GONE
                }
            }
        }
    }

    private fun IsOwnOverlay(packageName: String, activityName: String): Boolean {
        Log.d("CalendarAccessibilityService", "Package: $packageName, Activity: $activityName")
        return (packageName == "com.iceiony.visualcalendar" && activityName == "androidx.compose.ui.platform.ComposeView")
    }

    private fun isHomeScreen(packageName: String): Boolean {
        // Replace with more robust logic if needed
        return packageName in homePackages
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        savedStateBundle = Bundle().apply {
            savedStateRegistryController.performSave(this)
        }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        if (calendarView != null && windowManager != null) {
            windowManager?.removeView(calendarView)
            calendarView = null
        }

        super.onDestroy()

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

}

fun Int.dpToPx(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), context.resources.displayMetrics
    ).toInt()
}