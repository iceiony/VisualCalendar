package com.iceiony.visualcalendar

import biweekly.Biweekly
import biweekly.component.VEvent
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime

object iCalDataProvider {
    val icalUrl: String = BuildConfig.ICAL_DEBUG_URL

    private val client = OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(30))
        .build()

    fun today(now: LocalDateTime): List<VEvent> {
        val request = Request.Builder().url(icalUrl).get().build()
        val response = client.newCall(request).execute()

        val body = response.body?.string() ?: throw IOException("Empty body")
        val calendar = Biweekly.parse(body).first()           // parse to ICalendar
            ?: throw IllegalStateException("Invalid ICS feed")

        val today = now.toLocalDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC)
        val tomorrow = now.toLocalDate().plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)

        return calendar.events.filter() { event ->
            event.dateStart.value.toInstant().isAfter(today) &&
            event.dateStart.value.toInstant().isBefore(tomorrow)
        }
    }
}