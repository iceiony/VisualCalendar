package com.iceiony.visualcalendar.providers.google

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.work.WorkManager
import biweekly.component.VEvent
import biweekly.property.Attachment
import com.iceiony.visualcalendar.providers.SystemTimeProvider
import com.iceiony.visualcalendar.providers.TimeProvider
import com.iceiony.visualcalendar.VisualCalendarApp
import com.iceiony.visualcalendar.providers.AuthProvider
import com.iceiony.visualcalendar.providers.ScheduledDataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Date

class GoogleCalendarDataProvider(
    context: Context = VisualCalendarApp.instance.applicationContext,
    timeProvider: TimeProvider = SystemTimeProvider(),
    val authProvider: AuthProvider = VisualCalendarApp.instance.authProvider,
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

        Log.i("GoogleCalendarDataProvider", "Received calendar list response.")

        return (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            val id = item.getString("id")
            val summary = item.getString("summary")
            id to summary
        }.toMap()
    }

    override suspend fun getDaysEvents(now: LocalDateTime): List<VEvent> {
        val token = authProvider.getValidAccessToken()

        if( token.isNullOrEmpty()) {
            Log.w("GoogleCalendarDataProvider", "No valid access token available.")
            return emptyList()
        }

        val mainCalendar = getMainCalendar()

        if (mainCalendar.isNullOrEmpty()) {
            Log.w("GoogleCalendarDataProvider", "No main calendar set. Returning empty event list.")
            return emptyList()
        }

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
            val event = VEvent().apply {
                setSummary(item.optString("summary", "(No Title)"))
                setDateStart(parseDateTime(item.getJSONObject("start")))
                setDateEnd(parseDateTime(item.getJSONObject("end")))
            }

            val imageAttachment = extractImageUrl(item)
            if (imageAttachment != null) {
                event.addAttachment(imageAttachment)
            }

            event
        }
    }

    private fun parseDateTime(timeObj: JSONObject): Date {
        val dateStr = timeObj.optString("dateTime").ifEmpty { timeObj.getString("date") }
        return try {
            Date.from(Instant.parse(dateStr))
        } catch (e: Exception) {
            try {
                Date.from(OffsetDateTime.parse(dateStr).toInstant())
            } catch (e: Exception) {
                Date.from(java.time.LocalDate.parse(dateStr).atStartOfDay(ZoneId.systemDefault()).toInstant())
            }
        }
    }

    private fun extractImageUrl(item: JSONObject): Attachment? {
        if (item.has("attachments")) {
            val attachments = item.getJSONArray("attachments")

            for (i in 0 until attachments.length()) {
                val attachment = attachments.getJSONObject(i)

                if(
                    attachment.has("mimeType") &&
                    attachment.getString("mimeType").toString().startsWith("image/") &&
                    attachment.has("fileUrl")
                ) {
                    return Attachment(
                        attachment.getString("mimeType").toString(),
                        attachment.getString("fileUrl").toString()
                    )
                }
            }
        }

        return null
    }

    override fun setMainCalendar(calendarId: String) {
        prefs.edit {
            putString("calendar_id", calendarId)
        }
    }

    override suspend fun getMainCalendar() : String? {
        if (!prefs.contains("calendar_id")){
            return null
        }

        return prefs.getString("calendar_id", null) ?: throw Exception("No calendar selected")
    }

}