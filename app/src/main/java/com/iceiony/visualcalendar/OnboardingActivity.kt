package com.iceiony.visualcalendar

import android.os.Bundle
import android.util.Log
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
    lateinit var viewModel: PermissionsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("OnboardingActivity", "onCreate called")
        super.onCreate(savedInstanceState)

        viewModel = PermissionsViewModel(applicationContext)

        lifecycleScope.launch {
            snapshotFlow { viewModel.allGranted }
                .filter { it }
                .collect { finish() }

        }

        setContent {
            PermissionsChecklistView(viewModel = viewModel)
        }

    }

}


