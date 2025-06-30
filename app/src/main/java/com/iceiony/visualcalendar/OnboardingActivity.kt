package com.iceiony.visualcalendar

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri


class OnboardingActivity : AppCompatActivity() {
    private lateinit var overlayCaller: ActivityResultLauncher<Intent>
    private lateinit var accessibilityCaller: ActivityResultLauncher<Intent>

    var overlayPermissionsCallback = ActivityResultCallback<ActivityResult> {
        checkAllPermissionsGranted()

        if (!Permissions.isOverlayPermissionGranted(this)) {
            Toast.makeText(this, "Permission not granted to show overlay", Toast.LENGTH_SHORT).show()
        } else if ( !Permissions.isAccessibilityServiceEnabled(this) ) {
            requestAccessibilityPermissions()
        }
    }

    var accessibilityPermissionsCallback = ActivityResultCallback<ActivityResult> {
        checkAllPermissionsGranted()

        if (!Permissions.isAccessibilityServiceEnabled(this)) {
            Toast.makeText(this, "Permission not granted for accessibility service", Toast.LENGTH_SHORT).show()
        } else if (!Permissions.isOverlayPermissionGranted(this)) {
            requestOverlayPermissions()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAllPermissionsGranted()

        setContentView(R.layout.activity_main)
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
