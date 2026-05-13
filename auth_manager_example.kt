package com.iceiony.visualcalendar.providers

import android.content.Context
import com.iceiony.visualcalendar.BuildConfig
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.Duration

class GoogleAuthManager(context: Context) {

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

    data class CalendarInfo(val id: String, val summary: String)

    // Step 1: request a device code from Google
    fun requestDeviceCode(): DeviceCodeInfo {
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

        val json = JSONObject(response.body?.string() ?: throw Exception("Empty device code response"))
        return DeviceCodeInfo(
            deviceCode = json.getString("device_code"),
            userCode = json.getString("user_code"),
            verificationUrl = json.getString("verification_url"),
            intervalSeconds = json.getInt("interval"),
        )
    }

    // Step 2: poll until the user approves. Returns true when tokens are stored.
    // Returns false when still pending (caller should wait and retry).
    // Throws on unrecoverable errors (access_denied, expired_token, etc).
    fun pollForToken(deviceCode: String): Boolean {
        val response = client.newCall(
            Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(
                    FormBody.Builder()
                        .add("client_id", BuildConfig.GOOGLE_CLIENT_ID)
                        .add("client_secret", BuildConfig.GOOGLE_CLIENT_SECRET)
                        .add("device_code", deviceCode)
                        .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                        .build()
                )
                .build()
        ).execute()

        val json = JSONObject(response.body?.string() ?: throw Exception("Empty poll response"))
        if (json.has("error")) {
            when (val error = json.getString("error")) {
                "authorization_pending", "slow_down" -> return false
                else -> throw Exception("Authorization failed: $error")
            }
        }

        storeTokens(
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
            expiresIn = json.getLong("expires_in"),
        )
        return true
    }

    fun getValidAccessToken(): String {
        val expiry = prefs.getLong("token_expiry", 0L)
        if (expiry > System.currentTimeMillis() / 1000 + 60) {
            return prefs.getString("access_token", null) ?: throw Exception("No access token stored")
        }
        return refreshAccessToken()
    }

    private fun refreshAccessToken(): String {
        val refreshToken = prefs.getString("refresh_token", null)
            ?: throw Exception("Not authorized — no refresh token")

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
        if (json.has("error")) throw Exception("Token refresh failed: ${json.getString("error")}")

        storeTokens(
            accessToken = json.getString("access_token"),
            refreshToken = refreshToken,
            expiresIn = json.getLong("expires_in"),
        )
        return json.getString("access_token")
    }

    fun listCalendars(): List<CalendarInfo> {
        val token = getValidAccessToken()
        val response = client.newCall(
            Request.Builder()
                .url("https://www.googleapis.com/calendar/v3/calendarList?minAccessRole=reader")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
        ).execute()

        val json = JSONObject(response.body?.string() ?: throw Exception("Empty calendar list response"))
        val items = json.getJSONArray("items")
        return (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            CalendarInfo(
                id = item.getString("id"),
                summary = item.optString("summary", item.getString("id")),
            )
        }
    }

    fun isAuthorized() = prefs.contains("refresh_token")

    fun getCalendarId(): String? = prefs.getString("calendar_id", null)

    fun saveCalendarId(id: String) = prefs.edit().putString("calendar_id", id).apply()

    fun clearAuth() = prefs.edit().clear().apply()

    private fun storeTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putLong("token_expiry", System.currentTimeMillis() / 1000 + expiresIn)
            .apply()
    }
}
