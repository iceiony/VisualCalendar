package com.iceiony.visualcalendar.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iceiony.visualcalendar.Permissions
import com.iceiony.visualcalendar.R
import com.iceiony.visualcalendar.VisualCalendarApp
import com.iceiony.visualcalendar.providers.AuthProvider
import com.iceiony.visualcalendar.providers.DataProvider
import com.iceiony.visualcalendar.providers.google.GoogleAuthProvider
import com.iceiony.visualcalendar.providers.google.GoogleCalendarDataProvider
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.application
import kotlinx.coroutines.launch

class PermissionsViewModel(
    application: Application,
    val authProvider: AuthProvider = GoogleAuthProvider(application),
    val dataProvider: DataProvider = GoogleCalendarDataProvider(application, authProvider = authProvider)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        authProvider = GoogleAuthProvider(application),
        dataProvider = GoogleCalendarDataProvider(application, authProvider = GoogleAuthProvider(application))
    )

    //individual permission fields
    var isOverlayPermissionGranted: Boolean by mutableStateOf(
        Permissions.isOverlayPermissionGranted(application)
    )
    var isAccessibilityServiceEnabled: Boolean by mutableStateOf(
        Permissions.isAccessibilityServiceEnabled(application)
    )
    var isCalendarAccessGranted: Boolean by mutableStateOf(
        Permissions.isCalendarAccessGranted(application)
    )

    //authentication and calendar access
    var deviceCodeResponse: AuthProvider.DeviceCodeInfo? by mutableStateOf(null)
    var mainCalendar: String? by mutableStateOf(null)
    var calendars: Map<String, String> by mutableStateOf(emptyMap())
    val calendarSelectionCallback: (String) -> Unit = { calendarId ->
        dataProvider.setMainCalendar(calendarId)
        mainCalendar = calendarId
        isCalendarAccessGranted = true
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

        } else if ( !isAccessibilityServiceEnabled ) {
            requestAccessibilityPermissions()
        }
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
        } else if (!isOverlayPermissionGranted) {
            requestOverlayPermissions()
        }
    }

    private fun requestOverlayPermissions() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        )

        overlayCaller?.launch(intent)

        Toast.makeText(
            application,
            application.getString(R.string.grant_overlay_request),
            Toast.LENGTH_LONG
        ).show()

    }

    private fun requestAccessibilityPermissions() {
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
        isOverlayPermissionGranted && isAccessibilityServiceEnabled && isCalendarAccessGranted
    )

    init {
        //authentication tracking
        viewModelScope.launch {
            if(!authProvider.isAuthorised()) {
                authProvider.requestDeviceCode().collect {
                    deviceCodeResponse = it
                }
            } else {
                mainCalendar = dataProvider.getMainCalendar()
                calendars = dataProvider.calendars()
            }
        }
        //overlay and accessibility access
    }

    fun start() {
        if (!isOverlayPermissionGranted) {
            requestOverlayPermissions()
        } else if (!isAccessibilityServiceEnabled ) {
            requestAccessibilityPermissions()
        } else {
            allGranted = true
        }
    }

}