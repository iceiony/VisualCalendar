package com.iceiony.visualcalendar.providers.google

import android.content.Context
import android.content.Intent
import com.iceiony.visualcalendar.OnboardingActivity
import com.iceiony.visualcalendar.VisualCalendarApp
import com.iceiony.visualcalendar.local_storage.SecureStorage
import okhttp3.OkHttpClient
import java.time.Duration
import androidx.core.content.edit
import com.iceiony.visualcalendar.BuildConfig
import com.iceiony.visualcalendar.providers.AuthProvidier
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject

class GoogleAuthProvider(
    private val context: Context = VisualCalendarApp.instance.applicationContext,
    private val secureStorage: SecureStorage = SecureStorage(context)
) : AuthProvidier {
    private val prefs = context.getSharedPreferences("google_auth", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(30))
        .build()


    override fun requestDeviceCode(): Flow<AuthProvidier.DeviceCodeInfo> = flow {
        while (currentCoroutineContext().isActive) {
            val response = client.newCall(
                Request.Builder()
                    .url("https://oauth2.googleapis.com/device/code")
                    .post(
                        FormBody.Builder()
                            .add("client_id", BuildConfig.GOOGLE_CLIENT_ID)
                            .add("scope", "https://www.googleapis.com/auth/calendar.readonly")
                            .build()
                    )
                    .build()
            ).execute()

            val json =
                JSONObject(response.body?.string() ?: throw Exception("Empty device code response"))

            val deviceCode = AuthProvidier.DeviceCodeInfo(
                deviceCode = json.getString("device_code"),
                userCode = json.getString("user_code"),
                verificationUrl = json.getString("verification_url"),
                intervalSeconds = json.getInt("interval"),
                expiresIn = json.getLong("expires_in")
            )

            emit(deviceCode)

            delay(deviceCode.expiresIn * 1000L)
        }

    }

    suspend fun getValidAccessToken(): String? {
        //check if expiry token exists
        if( !prefs.contains("token_expiry") ) {
            //launch onboarding activity
            context.startActivity(
                Intent(context, OnboardingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )

            return null
        }

        val expiry = prefs.getLong("token_expiry", -1L)

        if (expiry < 0) {
            throw Exception("No token expiry stored")
        } else if (expiry > System.currentTimeMillis() / 1000 + 60) {
            return secureStorage.getValue("access_token") ?: throw Exception("No access token stored")
        }

        TODO()
    }

    suspend fun clearAuthState() {
        prefs.edit { clear() }
        secureStorage.deleteValue("access_token")
        secureStorage.deleteValue("refresh_token")
    }
}