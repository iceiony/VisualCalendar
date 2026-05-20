package com.iceiony.visualcalendar

import android.content.Context
import android.provider.Settings

object Permissions {

    fun allGranted(context: Context): Boolean {
        val canDrawOverlays =  isOverlayPermissionGranted(context)

        val accessibilityEnabled = isAccessibilityServiceEnabled(context)

        val calendarConfigured = isMainCalendarConfigured(context)

        val authenticated = isAuthenticated(context)

        return canDrawOverlays && accessibilityEnabled && calendarConfigured && authenticated
    }

    fun isOverlayPermissionGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false

        return enabledServices.contains(context.packageName)
    }

    fun isAuthenticated(context: Context): Boolean {
        return context
            .getSharedPreferences("google_auth", Context.MODE_PRIVATE)
            .contains("token_expiry")
    }

    fun isMainCalendarConfigured(context: Context): Boolean {
        return context
            .getSharedPreferences("google_calendar", Context.MODE_PRIVATE)
            .contains("calendar_id")
    }

}