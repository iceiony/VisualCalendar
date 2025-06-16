package com.iceiony.visualcalendar

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {
    private lateinit var overlayPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        overlayPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (Settings.canDrawOverlays(this)) {
                startAccessibilityService()
            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
            }
        }

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
