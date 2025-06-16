package com.iceiony.visualcalendar

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {
    lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>
    var overlayPermissionCallback = ActivityResultCallback<ActivityResult> {
        if (Settings.canDrawOverlays(this)) {
            startAccessibilityService()
        } else {
            Toast.makeText(this, "Permission not granted to show overlay", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
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
        } else {
            startAccessibilityService()
        }
    }
    private fun startAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        Toast.makeText(
            this,
            "Please enable Visual Calendar in Accessibility Services",
            Toast.LENGTH_LONG
        ).show()
        startActivity(intent)
    }
}
