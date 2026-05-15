package com.iceiony.visualcalendar

import android.content.Context
import android.provider.Settings
import android.util.Log

object Permissions {

    fun allGranted(context: Context): Boolean {
        val canDrawOverlays =  isOverlayPermissionGranted(context)
        //Log.d("Permissions", "Overlay permission granted: $canDrawOverlays")

        val accessibilityEnabled = isAccessibilityServiceEnabled(context)
        //Log.d("Permissions", "Accessibility service enabled: $accessibilityEnabled")

        val calendarAccessGranted = isCalendarAccessGranted(context)

        return canDrawOverlays && accessibilityEnabled && calendarAccessGranted
    }

    fun isOverlayPermissionGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false

        return enabledServices.contains(context.packageName)
    }

    fun isCalendarAccessGranted(context: Context): Boolean {
        return context
            .getSharedPreferences("google_auth", Context.MODE_PRIVATE)
            .contains("calendar_id")
    }

}