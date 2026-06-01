package com.iceiony.visualcalendar.viewmodels

import android.app.Application
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iceiony.visualcalendar.Permissions
import com.iceiony.visualcalendar.R
import com.iceiony.visualcalendar.providers.AuthProvider
import com.iceiony.visualcalendar.providers.DataProvider
import com.iceiony.visualcalendar.providers.google.GoogleAuthProvider
import com.iceiony.visualcalendar.providers.google.GoogleCalendarDataProvider
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.application
import com.iceiony.visualcalendar.VisualCalendarApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

class PermissionsViewModel(
    application: Application,
    val authProvider: AuthProvider = VisualCalendarApp.instance.authProvider,
    val dataProvider: DataProvider = VisualCalendarApp.instance.dataProvider
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        authProvider = VisualCalendarApp.instance.authProvider,
        dataProvider = VisualCalendarApp.instance.dataProvider
    )

    //individual permission fields
    var isOverlayPermissionGranted: Boolean by mutableStateOf(
        Permissions.isOverlayPermissionGranted(application)
    )
    var isAccessibilityServiceEnabled: Boolean by mutableStateOf(
        Permissions.isAccessibilityServiceEnabled(application)
    )
    var isCalendarAccessGranted: Boolean by mutableStateOf(
        authProvider.isAuthorised()
    )

    var isCalendarSelected: Boolean by mutableStateOf(
        Permissions.isMainCalendarConfigured(application)
    )

    //authentication and calendar access
    var deviceCodeResponse: AuthProvider.DeviceCodeInfo? by mutableStateOf(null)
    var mainCalendar: String? by mutableStateOf(null)
    var calendars: Map<String, String> by mutableStateOf(emptyMap())
    val calendarSelectionCallback: (String) -> Unit = { calendarId ->
        dataProvider.setMainCalendar(calendarId)
        mainCalendar = calendarId

        viewModelScope.launch {
            dataProvider.refresh()
        }

        isCalendarSelected = true
        checkAllPermissions()
    }

    //overlay permission callback
    var overlayCaller: ActivityResultLauncher<Intent>? = null
    var accessibilityCaller: ActivityResultLauncher<Intent>? = null

    val overlayPermissionsCallback = ActivityResultCallback<ActivityResult> {
        isOverlayPermissionGranted = Permissions.isOverlayPermissionGranted(application)

        if (!isOverlayPermissionGranted) {
            Toast
                .makeText(
                    application,
                    "Permission not granted to show overlay",
                    Toast.LENGTH_SHORT)
                .show()

        }

        checkAllPermissions()
    }

    val accessibilityPermissionsCallback = ActivityResultCallback<ActivityResult> {
        isAccessibilityServiceEnabled = Permissions.isAccessibilityServiceEnabled(application)

        if (!isAccessibilityServiceEnabled) {
            Toast
                .makeText(
                    application,
                    "Permission not granted for accessibility service",
                    Toast.LENGTH_SHORT)
                .show()
        }

        checkAllPermissions()
    }

    fun requestOverlayPermissions() {
        val intent = Intent( Settings.ACTION_MANAGE_OVERLAY_PERMISSION)

        overlayCaller?.launch(intent)

        Toast.makeText(
            application,
            application.getString(R.string.grant_overlay_request),
            Toast.LENGTH_LONG
        ).show()

    }

    fun requestAccessibilityPermissions() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

        accessibilityCaller?.launch(intent)

        Toast.makeText(
            application,
            application.getString(R.string.grant_accessibility_request),
            Toast.LENGTH_LONG
        ).show()

    }

    // all done
    var allGranted : Boolean by mutableStateOf(
        isOverlayPermissionGranted &&
               isAccessibilityServiceEnabled &&
               isCalendarAccessGranted &&
               isCalendarSelected )

    fun start() {
        //authentication tracking
        viewModelScope.launch(Dispatchers.IO) {
            if(!isCalendarAccessGranted) {
                authProvider
                    .requestDeviceCode()
                    .collect { deviceCodeResponse = it }
            }

            calendars = dataProvider.calendars()
            mainCalendar = dataProvider.getMainCalendar()

            isCalendarAccessGranted = authProvider.isAuthorised()
            checkAllPermissions()
        }
        //overlay and accessibility access
    }

    fun checkAllPermissions(): Boolean {
        allGranted = isOverlayPermissionGranted &&
                isAccessibilityServiceEnabled &&
                isCalendarAccessGranted &&
                isCalendarSelected

        return allGranted
    }
}