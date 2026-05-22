package com.iceiony.visualcalendar.providers.google

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.work.WorkManager
import biweekly.component.VEvent
import com.iceiony.visualcalendar.SystemTimeProvider
import com.iceiony.visualcalendar.TimeProvider
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import java.time.LocalDateTime
import com.iceiony.visualcalendar.VisualCalendarApp
import com.iceiony.visualcalendar.providers.ScheduledDataProvider
import okhttp3.Request
import com.iceiony.visualcalendar.providers.AuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.util.Date

class GoogleCalendarDataProvider(
    context: Context = VisualCalendarApp.instance.applicationContext,
    timeProvider: TimeProvider = SystemTimeProvider(),
    val authProvider: AuthProvider = GoogleAuthProvider(context),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    client: okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder().callTimeout(java.time.Duration.ofSeconds(30)).build()
) : ScheduledDataProvider(
    workManager = WorkManager.getInstance(context.applicationContext),
    timeProvider, scope , client
) {
    val prefs = context.getSharedPreferences("google_calendar", Context.MODE_PRIVATE)

    override suspend fun calendars(): Map<String, String> {
        val token = authProvider.getValidAccessToken()

        //https://www.googleapis.com/calendar/v3/users/me/calendarList
        val response = client.newCall(
            Request.Builder()
                .url("https://www.googleapis.com/calendar/v3/users/me/calendarList")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
        ).execute()

        val body = response.body?.string() ?: throw Exception("Empty calendar response")

        val items = JSONObject(body).getJSONArray("items")

        Log.i("GoogleCalendarDataProvider", "Received calendar list response: $body")

        return (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            val id = item.getString("id")
            val summary = item.getString("summary")
            id to summary
        }.toMap()
    }

    override suspend fun getDaysEvents(now: LocalDateTime): List<VEvent> {
        val token = authProvider.getValidAccessToken()
        val mainCalendar = getMainCalendar()

        val zone = ZoneId.systemDefault()
        val dayStart = now.toLocalDate().atStartOfDay(zone).toInstant()
        val dayEnd = now.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()

        val url = "https://www.googleapis.com/calendar/v3/calendars/" +
                "${URLEncoder.encode(mainCalendar, "UTF-8")}/events" +
                "?timeMin=${URLEncoder.encode(dayStart.toString(), "UTF-8")}" +
                "&timeMax=${URLEncoder.encode(dayEnd.toString(), "UTF-8")}" +
                "&singleEvents=true&orderBy=startTime&maxResults=50"

        val response = client.newCall(
            Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
        ).execute()

        val body = response.body?.string() ?: throw Exception("Empty calendar response")
        val items = JSONObject(body).getJSONArray("items")

        Log.d("GoogleCalendarDataProvider", "Received ${items.length()} events for calendar $mainCalendar")
        return (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            VEvent().apply {
                setSummary(item.optString("summary", "(No Title)"))
                setDateStart(parseDateTime(item.getJSONObject("start")))
                setDateEnd(parseDateTime(item.getJSONObject("end")))
                //extractImageUrl(item)?.let { addExperimentalProperty("X-IMAGE-URL", it) }
            }
        }
    }

    private fun parseDateTime(timeObj: JSONObject): Date {
        val dateStr = timeObj.optString("dateTime").ifEmpty { timeObj.getString("date") }
        return try {
            Date.from(Instant.parse(dateStr))
        } catch (e: Exception) {
            Date.from(java.time.LocalDate.parse(dateStr).atStartOfDay(ZoneId.systemDefault()).toInstant())
        }
    }

    private fun extractImageUrl(item: JSONObject): String? {
        if (item.has("attachments")) {
            val attachments = item.getJSONArray("attachments")
            for (i in 0 until attachments.length()) {
                val attachment = attachments.getJSONObject(i)
                if (attachment.optString("FMTTYPE").startsWith("image/")) {
                    return attachment.optString("fileUrl").takeIf { it.isNotEmpty() }
                }
            }
        }
        val description = item.optString("description")
        return Regex("""\[image](https?://\S+)""").find(description)?.groupValues?.get(1)
    }

    override fun setMainCalendar(calendarId: String) {
        prefs.edit {
            putString("calendar_id", calendarId)
        }
    }

    override suspend fun getMainCalendar() : String {
        if (!prefs.contains("calendar_id")){
            val calendarList = calendars()

            if (calendarList.isEmpty()) throw Exception("No calendars found for user")

            setMainCalendar(calendarList.keys.first())
        }

        return prefs.getString("calendar_id", null) ?: throw Exception("No calendar selected")
    }

}