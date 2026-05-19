package com.iceiony.visualcalendar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.lifecycleScope
import com.iceiony.visualcalendar.Permissions.isAccessibilityServiceEnabled
import com.iceiony.visualcalendar.Permissions.isCalendarAccessGranted
import com.iceiony.visualcalendar.Permissions.isOverlayPermissionGranted
import com.iceiony.visualcalendar.viewmodels.PermissionsViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {
    val viewModel: PermissionsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Permissions.allGranted(application)) {
            finish()
            return
        }

        registerPermissionsLaunchers()

        lifecycleScope.launch {
            snapshotFlow { viewModel.allGranted }
                .filter { it }
                .collect { finish() }

        }

        setContent {
            PermissionsChecklistView(viewModel = viewModel)
        }

        viewModel.start()
    }

    private fun registerPermissionsLaunchers() {
        viewModel.overlayCaller = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            viewModel.overlayPermissionsCallback
        )

        viewModel.accessibilityCaller = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            viewModel.accessibilityPermissionsCallback
        )
    }

}


