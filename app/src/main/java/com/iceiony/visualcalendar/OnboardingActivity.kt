package com.iceiony.visualcalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.lifecycleScope
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


