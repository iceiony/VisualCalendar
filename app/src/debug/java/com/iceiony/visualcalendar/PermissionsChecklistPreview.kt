package com.iceiony.visualcalendar

import android.annotation.SuppressLint
import androidx.compose.ui.tooling.preview.Preview
import biweekly.component.VEvent
import com.iceiony.visualcalendar.providers.DataProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime

import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable

import com.iceiony.visualcalendar.providers.AuthProvider
import com.iceiony.visualcalendar.viewmodels.PermissionsViewModel


@SuppressLint("UnrememberedMutableState")
@Preview()
@Composable
fun PermissionsChecklistPreview() {
    val context = LocalContext.current
    val authProvider = object : AuthProvider {
        override fun requestDeviceCode(): Flow<AuthProvider.DeviceCodeInfo> = flow {
            emit(
                AuthProvider.DeviceCodeInfo(
                    deviceCode = "device_code",
                    userCode = "user_code",
                    verificationUrl = "https://example.com/verify",
                    intervalSeconds = 5,
                    expiresIn = 300L
                )
            )
        }

        override suspend fun getValidAccessToken(): String?  =  null
        override fun isAuthorised(): Boolean = false
    }

    val calendarProvider = object : DataProvider {
        override suspend fun calendars(): Map<String, String> = emptyMap()
        override suspend fun getMainCalendar(): String = "main_calendar_id"
        override fun today(): StateFlow<List<VEvent>> {
            return MutableStateFlow<List<VEvent>>(emptyList())
        }
        override suspend fun refresh(now: LocalDateTime?) {}
        override fun setMainCalendar(calendarId: String) { }
        override fun destroy() { }

    }

    val viewModel = PermissionsViewModel(context, authProvider, calendarProvider)

    PermissionsChecklistView( viewModel = viewModel )
}
