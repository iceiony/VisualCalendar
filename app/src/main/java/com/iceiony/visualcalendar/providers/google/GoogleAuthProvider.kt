package com.iceiony.visualcalendar.providers.google

import android.content.Context
import android.content.Intent
import com.iceiony.visualcalendar.OnboardingActivity
import com.iceiony.visualcalendar.VisualCalendarApp
import com.iceiony.visualcalendar.local_storage.SecureStorage
import okhttp3.OkHttpClient
import java.time.Duration

class GoogleAuthProvider(
    private val context: Context = VisualCalendarApp.instance.applicationContext,
    private val secureStorage: SecureStorage = SecureStorage(context)
) {
    private val prefs = context.getSharedPreferences("google_auth", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(30))
        .build()

    data class DeviceCodeInfo(
        val deviceCode: String,
        val userCode: String,
        val verificationUrl: String,
        val intervalSeconds: Int,
    )

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
        prefs.edit().clear().apply()
        secureStorage.deleteValue("access_token")
        secureStorage.deleteValue("refresh_token")
    }
}