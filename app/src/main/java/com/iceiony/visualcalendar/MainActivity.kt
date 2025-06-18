package com.iceiony.visualcalendar

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
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
    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>
    var overlayPermissionCallback = ActivityResultCallback<ActivityResult> {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permission not granted to show overlay", Toast.LENGTH_SHORT).show()
        } else if (
            !isAccessibilityServiceEnabled(this)
        ) {
            requestAccessibilityPermissions()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlayPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            overlayPermissionCallback
        )

        // Check and request overlay permission
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            overlayPermissionLauncher.launch(intent)

            Toast.makeText(
                this,
                "Please enable overlay permission for Visual Calendar",
                Toast.LENGTH_LONG
            ).show()

        } else if (
            !isAccessibilityServiceEnabled(this)
        ) {
            requestAccessibilityPermissions()
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false

        return enabledServices.contains(context.packageName)
    }
    private fun requestAccessibilityPermissions() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        Toast.makeText(
            this,
            "Please enable Visual Calendar in Accessibility Services",
            Toast.LENGTH_LONG
        ).show()
        startActivity(intent)
    }
}
