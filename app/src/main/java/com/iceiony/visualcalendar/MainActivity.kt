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

class MainActivity : AppCompatActivity() {
    private lateinit var overlayCaller: ActivityResultLauncher<Intent>
    private lateinit var accessibilityCaller: ActivityResultLauncher<Intent>

    var overlayPermissionsCallback = ActivityResultCallback<ActivityResult> {
        checkAllPermissionsGranted()

        if (!isOverlayPermissionGranted()) {
            Toast.makeText(this, "Permission not granted to show overlay", Toast.LENGTH_SHORT).show()
        } else if ( !isAccessibilityServiceEnabled() ) {
            requestAccessibilityPermissions()
        }
    }

    var accessibilityPermissionsCallback = ActivityResultCallback<ActivityResult> {
        checkAllPermissionsGranted()

        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "Permission not granted for accessibility service", Toast.LENGTH_SHORT).show()
        } else if (!isOverlayPermissionGranted()) {
            requestOverlayPermissions()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAllPermissionsGranted()

        setContentView(R.layout.activity_main)
        registerPermissionsLaunchers()

        if (!isOverlayPermissionGranted()) {
            requestOverlayPermissions()
        } else if ( !isAccessibilityServiceEnabled() ) {
            requestAccessibilityPermissions()
        }
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(this.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false

        return enabledServices.contains(this.packageName)
    }

    private fun checkAllPermissionsGranted(): Boolean {
        if(isOverlayPermissionGranted() && isAccessibilityServiceEnabled()) {
            startActivity(Intent(this, CalendarActivity::class.java))
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
