package com.iceiony.visualcalendar.providers.google

import android.content.Context
import android.content.Intent
import android.util.Log
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
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable

class GoogleAuthProvider(
    val context: Context = VisualCalendarApp.instance.applicationContext,
    val secureStorage: SecureStorage = SecureStorage(context),
    val oauthURL : String = "https://oauth2.googleapis.com",
    val client: OkHttpClient = OkHttpClient.Builder().callTimeout(Duration.ofSeconds(30)).build()
) : AuthProvidier {
    val prefs = context.getSharedPreferences("google_auth", Context.MODE_PRIVATE)

    override fun requestDeviceCode(): Flow<AuthProvidier.DeviceCodeInfo> = channelFlow {
        while (currentCoroutineContext().isActive) {
            val response = client.newCall(
                Request.Builder()
                    .url("$oauthURL/device/code")
                    .post(
                        FormBody.Builder()
                            .add("client_id", BuildConfig.GOOGLE_CLIENT_ID)
                            .add("scope", "https://www.googleapis.com/auth/calendar.readonly")
                            .build()
                    )
                    .build()
            ).execute()

            val json = JSONObject(response.body?.string() ?: throw Exception("Empty device code response"))

            val deviceCode = AuthProvidier.DeviceCodeInfo(
                deviceCode = json.getString("device_code"),
                userCode = json.getString("user_code"),
                verificationUrl = json.getString("verification_url"),
                intervalSeconds = json.getInt("interval"),
                expiresIn = json.getLong("expires_in")
            )

            send(deviceCode)

            val pollJob = launch {
                if (pollForToken(deviceCode)) {
                    close()
                }
            }

            delay(deviceCode.expiresIn * 1000L)

            pollJob.cancel()
        }

    }

    suspend fun pollForToken(deviceCode: AuthProvidier.DeviceCodeInfo): Boolean {
        while (currentCoroutineContext().isActive) {
            delay(deviceCode.intervalSeconds * 1000L)

            val response = client.newCall(
                Request.Builder()
                    .url("$oauthURL/token")
                    .post(
                        FormBody.Builder()
                            .add("client_id", BuildConfig.GOOGLE_CLIENT_ID)
                            .add("client_secret", BuildConfig.GOOGLE_CLIENT_SECRET)
                            .add("device_code", deviceCode.deviceCode)
                            .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                            .build()
                    )
                    .build()
            ).execute()

            val json = JSONObject(response.body?.string() ?: throw Exception("Empty poll response"))
            if (json.has("error")) {
                when (val error = json.getString("error")) {
                    "authorization_pending", "slow_down" -> continue
                    else -> {
                        Log.e( "GoogleAuthProvider", "Authorization failed: $error")
                        return false
                    }
                }
            }

            withContext(NonCancellable) {
                secureStorage.saveValue("access_token", json.getString("access_token"))
                secureStorage.saveValue("refresh_token", json.getString("refresh_token"))
                prefs.edit {
                    putLong(
                        "token_expiry",
                        System.currentTimeMillis() / 1000 + json.getLong("expires_in")
                    )
                }
            }

            return true
        }

        return false
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