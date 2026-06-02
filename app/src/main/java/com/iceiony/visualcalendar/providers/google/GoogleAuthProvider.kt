package com.iceiony.visualcalendar.providers.google

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import com.iceiony.visualcalendar.BuildConfig
import com.iceiony.visualcalendar.OnboardingActivity
import com.iceiony.visualcalendar.providers.AuthProvider
import com.iceiony.visualcalendar.providers.SecureStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Duration

class GoogleAuthProvider(
    val context: Context,
    val secureStorage: SecureStorage = SecureStorage(context),
    val oauthURL : String = "https://oauth2.googleapis.com",
    val client: OkHttpClient = OkHttpClient.Builder().callTimeout(Duration.ofSeconds(30)).build()
) : AuthProvider {
    val prefs = context.getSharedPreferences("google_auth", Context.MODE_PRIVATE)

    private var loginDeferred: CompletableDeferred<Boolean>? = null

    override fun requestDeviceCode(): Flow<AuthProvider.DeviceCodeInfo> = channelFlow {
        if(loginDeferred == null) {
            loginDeferred = CompletableDeferred()
        }

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

            val deviceCode = AuthProvider.DeviceCodeInfo(
                deviceCode = json.getString("device_code"),
                userCode = json.getString("user_code"),
                verificationUrl = json.getString("verification_url"),
                intervalSeconds = json.getInt("interval"),
                expiresIn = json.getLong("expires_in"),
            )

            send(deviceCode)

            val pollJob = launch {
                if (pollForToken(deviceCode)) {
                    //send(deviceCode)
                    try {
                        cancel()
                        close()
                    } catch (e: Exception) {
                        Log.e("GoogleAuthProvider", "Error closing device code flow: ${e.message}")
                    }
                } else {
                    Log.e("GoogleAuthProvider", "Polling for token failed")
                }
            }

            delay(deviceCode.expiresIn * 1000L)

            //pollJob.cancel()
        }

    }

    suspend fun pollForToken(deviceCode: AuthProvider.DeviceCodeInfo): Boolean {
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

            return setAuthState(json)
        }

        return true
    }

    override suspend fun getValidAccessToken(): String? {
        //check if expiry token exists
        if(
            !isAuthorised() && loginDeferred == null
        ) {
            loginDeferred = CompletableDeferred()
            context.startActivity(
                Intent(context, OnboardingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }

        loginDeferred?.await()

        if ( !isAuthorised() ) {
            Log.w("GoogleAuthProvider", "Not authorized after login attempt")
            return null
        }

        val expiry = prefs.getLong("token_expiry", -1L)

        if (expiry < 0) {
            throw Exception("No token expiry stored")
        } else if (expiry > System.currentTimeMillis() / 1000) {
            return secureStorage.getValue("access_token") ?: throw Exception("No access token stored")
        }

        val refreshToken = secureStorage.getValue("refresh_token") ?: throw Exception("Not authorized — no refresh token")
        val response = client.newCall(
            Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(
                    FormBody.Builder()
                        .add("client_id", BuildConfig.GOOGLE_CLIENT_ID)
                        .add("client_secret", BuildConfig.GOOGLE_CLIENT_SECRET)
                        .add("refresh_token", refreshToken)
                        .add("grant_type", "refresh_token")
                        .build()
                )
                .build()
        ).execute()

        val json = JSONObject(response.body?.string() ?: throw Exception("Empty refresh response"))
        if (json.has("error")) {
            //check if token revoked
            if (json.getString("error") == "invalid_grant") {
                Log.w("GoogleAuthProvider", "Refresh token invalid, clearing auth state")
                
                withContext(NonCancellable) {
                    clearAuthState()
                }

                return getValidAccessToken()
            }

            throw Exception("Token refresh failed: ${json.getString("error")}")
        }

        withContext(NonCancellable) {
            prefs.edit {
                putLong(
                    "token_expiry",
                    System.currentTimeMillis() / 1000 + json.getLong("expires_in")
                )
            }

            secureStorage.saveValue("access_token", json.getString("access_token"))
        }

        return json.getString("access_token")
    }

    override fun isAuthorised(): Boolean {
        return prefs.contains("token_expiry")
    }

    suspend fun setAuthState( json: JSONObject ) : Boolean {
        prefs.edit {
            putLong(
                "token_expiry",
                System.currentTimeMillis() / 1000 + json.getLong("expires_in")
            )
        }

        secureStorage.saveValue("access_token", json.getString("access_token"))
        secureStorage.saveValue("refresh_token", json.getString("refresh_token"))

        loginDeferred?.complete(true)
        loginDeferred = null

        return true
    }

    suspend fun clearAuthState() {
        prefs.edit { clear() }
        secureStorage.deleteValue("access_token")
        secureStorage.deleteValue("refresh_token")
    }
}