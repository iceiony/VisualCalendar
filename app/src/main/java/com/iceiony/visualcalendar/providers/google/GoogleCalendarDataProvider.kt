package com.iceiony.visualcalendar.providers.google

import android.content.Context
import android.util.Log
import androidx.core.content.edit
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

class GoogleCalendarDataProvider(
    context: Context = VisualCalendarApp.instance.applicationContext,
    timeProvider: TimeProvider = SystemTimeProvider(),
    scheduler : Scheduler = Schedulers.io(),
    val authProvider: AuthProvider = GoogleAuthProvider(context),
) : ScheduledDataProvider(timeProvider, scheduler) {
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

        val items = org.json.JSONObject(body).getJSONArray("items")

        Log.i("GoogleCalendarDataProvider", "Received calendar list response: $body")

        return (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            val id = item.getString("id")
            val summary = item.getString("summary")
            id to summary
        }.toMap()
    }

    override fun getDaysEvents(now: LocalDateTime): List<VEvent> {
        //val token = authProvider.getValidAccessToken()
        TODO("Not yet implemented")
    }

    override fun setMainCalendar(calendarId: String) {
        prefs.edit {
            putString("calendar_id", calendarId)
        }
    }

    override suspend fun getMainCalendar() : String? {
        if (!prefs.contains("calendar_id")){
            val calendarList = calendars()

            if (calendarList.isEmpty()) throw Exception("No calendars found for user")

            setMainCalendar(calendarList.keys.first())
        }

        return prefs.getString("calendar_id", null)
    }

}