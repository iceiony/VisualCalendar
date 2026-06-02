package com.iceiony.visualcalendar.viewmodels

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.iceiony.visualcalendar.Permissions
import com.iceiony.visualcalendar.R
import com.iceiony.visualcalendar.VisualCalendarApp
import com.iceiony.visualcalendar.providers.AuthProvider
import com.iceiony.visualcalendar.providers.DataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PermissionsViewModel(
    context: Context,
    val authProvider: AuthProvider = VisualCalendarApp.instance.authProvider,
    val dataProvider: DataProvider = VisualCalendarApp.instance.dataProvider,
    val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) : ViewModel() {

    constructor(context: Context) : this(
        context,
        authProvider = VisualCalendarApp.instance.authProvider,
        dataProvider = VisualCalendarApp.instance.dataProvider
    )

    //individual permission fields
    var isOverlayPermissionGranted: Boolean by mutableStateOf(
        Permissions.isOverlayPermissionGranted(context)
    )
    var isAccessibilityServiceEnabled: Boolean by mutableStateOf(
        Permissions.isAccessibilityServiceEnabled(context)
    )
    var isCalendarAccessGranted: Boolean by mutableStateOf(
        authProvider.isAuthorised()
    )
    var isCalendarSelected: Boolean by mutableStateOf(
        Permissions.isMainCalendarConfigured(context)
    )

    //authentication and calendar access
    var deviceCodeResponse: AuthProvider.DeviceCodeInfo? by mutableStateOf(null)
    var mainCalendar: String? by mutableStateOf(null)
    var calendars: Map<String, String> by mutableStateOf(emptyMap())
    val calendarSelectionCallback: (String) -> Unit = { calendarId ->
        dataProvider.setMainCalendar(calendarId)
        mainCalendar = calendarId

        scope.launch {
            dataProvider.refresh()
        }

        isCalendarSelected = true
        checkAllPermissions()
    }

    //overlay permission callback
    var overlayCaller: ActivityResultLauncher<Intent>? = null
    var accessibilityCaller: ActivityResultLauncher<Intent>? = null

    val overlayPermissionsCallback = ActivityResultCallback<ActivityResult> {
        isOverlayPermissionGranted = Permissions.isOverlayPermissionGranted(context)

        if (!isOverlayPermissionGranted) {
            Toast
                .makeText(
                    context,
                    "Permission not granted to show overlay",
                    Toast.LENGTH_SHORT)
                .show()

        }

        checkAllPermissions()
    }

    val accessibilityPermissionsCallback = ActivityResultCallback<ActivityResult> {
        isAccessibilityServiceEnabled = Permissions.isAccessibilityServiceEnabled(context)

        if (!isAccessibilityServiceEnabled) {
            Toast
                .makeText(
                    context,
                    "Permission not granted for accessibility service",
                    Toast.LENGTH_LONG)
                .show()
        }

        checkAllPermissions()
    }

    fun requestOverlayPermissions(context: Context) {
        val intent = Intent( Settings.ACTION_MANAGE_OVERLAY_PERMISSION)

        overlayCaller?.launch(intent)

        Toast
            .makeText(
                context,
                context.applicationContext.getString(R.string.grant_overlay_request),
                Toast.LENGTH_LONG)
            .show()
    }

    fun requestAccessibilityPermissions(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

        accessibilityCaller?.launch(intent)

        context.applicationContext

        Toast.makeText(
            context,
            context.applicationContext.getString(R.string.grant_accessibility_request),
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
        scope.launch(Dispatchers.IO) {
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