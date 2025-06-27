package com.iceiony.visualcalendar

import android.content.Context
import android.provider.Settings

object Permissions {

    fun allGranted(context: Context): Boolean {
        return isOverlayPermissionGranted(context) && isAccessibilityServiceEnabled(context)
    }

    fun isOverlayPermissionGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false

        return enabledServices.contains(context.packageName)
    }

}