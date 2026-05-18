package com.iceiony.visualcalendar

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.iceiony.visualcalendar.Permissions.isAccessibilityServiceEnabled
import com.iceiony.visualcalendar.Permissions.isCalendarAccessGranted
import com.iceiony.visualcalendar.Permissions.isOverlayPermissionGranted

class OnboardingActivity : AppCompatActivity() {
    private lateinit var overlayCaller: ActivityResultLauncher<Intent>
    private lateinit var accessibilityCaller: ActivityResultLauncher<Intent>

    private var permissionUpdates by mutableStateOf<PermissionUpdates>(
        PermissionUpdates(
            isOverlayPermissionGranted = false,
            isAccessibilityServiceEnabled = false,
            isCalendarAccessGranted =false
        )
    )
    lateinit var overlayPermissionsCallback:  ActivityResultCallback<ActivityResult>
    lateinit var accessibilityPermissionsCallback: ActivityResultCallback<ActivityResult>

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("OnboardingActivity", "onCreate called")
        super.onCreate(savedInstanceState)

        permissionUpdates = Permissions.updates(this)

        overlayPermissionsCallback = ActivityResultCallback<ActivityResult> {
            permissionUpdates = Permissions.updates(this)

            checkAllPermissionsGranted()

            if (!Permissions.isOverlayPermissionGranted(this)) {
                Toast.makeText(this, "Permission not granted to show overlay", Toast.LENGTH_SHORT).show()
            } else if ( !Permissions.isAccessibilityServiceEnabled(this) ) {
                requestAccessibilityPermissions()
            }
        }

        accessibilityPermissionsCallback = ActivityResultCallback<ActivityResult> {
            permissionUpdates = Permissions.updates(this)

            checkAllPermissionsGranted()

            if (!Permissions.isAccessibilityServiceEnabled(this)) {
                Toast.makeText(this, "Permission not granted for accessibility service", Toast.LENGTH_SHORT).show()
            } else if (!Permissions.isOverlayPermissionGranted(this)) {
                requestOverlayPermissions()
            }
        }

        checkAllPermissionsGranted()

        setContentView(R.layout.activity_onboarding)

        findViewById<ComposeView>(R.id.permissions_checklist_view).setContent {
            PermissionsChecklistView(permissionUpdates = permissionUpdates)
        }

        registerPermissionsLaunchers()

        if (!Permissions.isOverlayPermissionGranted(this)) {
            requestOverlayPermissions()
        } else if ( !Permissions.isAccessibilityServiceEnabled(this) ) {
            requestAccessibilityPermissions()
        }
    }

    private fun checkAllPermissionsGranted(): Boolean {
        if(Permissions.allGranted(this)) {
            finish()
            return true
        }
        return false
    }

    private fun registerPermissionsLaunchers() {
        overlayCaller = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            overlayPermissionsCallback
        )

        accessibilityCaller = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            accessibilityPermissionsCallback
        )
    }

    private fun requestOverlayPermissions() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri()
        )

        overlayCaller.launch(intent)

        Toast.makeText(
            this,
            getString(R.string.grant_overlay_request),
            Toast.LENGTH_LONG
        ).show()

    }

    private fun requestAccessibilityPermissions() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

        accessibilityCaller.launch(intent)

        Toast.makeText(
            this,
            getString(R.string.grant_accessibility_request),
            Toast.LENGTH_LONG
        ).show()

    }
}
