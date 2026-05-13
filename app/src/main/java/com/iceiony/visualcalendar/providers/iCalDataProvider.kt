package com.iceiony.visualcalendar.providers

import android.annotation.SuppressLint
import biweekly.Biweekly
import biweekly.component.VEvent
import com.iceiony.visualcalendar.BuildConfig
import com.iceiony.visualcalendar.SystemTimeProvider
import com.iceiony.visualcalendar.TimeProvider
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone

@SuppressLint("CheckResult")
class iCalDataProvider(
    timeProvider: TimeProvider = SystemTimeProvider(),
    scheduler : Scheduler = Schedulers.io(),
    private val iCalUrl: String = BuildConfig.ICAL_DEBUG_URL
) : ScheduledDataProvider(timeProvider, scheduler) {

override fun getDaysEvents(now: LocalDateTime): List<VEvent> {
    val request = Request.Builder().url(iCalUrl).get().build()
    val response = client.newCall(request).execute()

    val body = response.body?.string() ?: throw IOException("Empty body")
    val calendar = Biweekly.parse(body).first()           // parse to ICalendar
        ?: throw IllegalStateException("Invalid ICS feed")

    val today = now
        .toLocalDate()
        .atStartOfDay()
        .atZone(ZoneId.systemDefault())
        .toInstant()

    val tomorrow = now
        .toLocalDate()
        .plusDays(1)
        .atStartOfDay()
        .atZone(ZoneId.systemDefault())
        .toInstant()

    val oneOffEvents =  calendar.events.filter() { event ->
        val start = event.dateStart.value.toInstant()
        start.isAfter(today) && start.isBefore(tomorrow)
    }

    val reoccurringEvents = calendar.events.filter() {
        it.recurrenceRule != null
    }.map { event ->
        val nextDates = event.getDateIterator(TimeZone.getDefault())

        while(nextDates.hasNext()) {
            val date = nextDates.next()
            if(date.toInstant().isBefore(today)) continue
            if(date.toInstant().isAfter(tomorrow)) break

            event.setDateEnd( Date(date.time + (event.dateEnd.value.time - event.dateStart.value.time)) )
            event.setDateStart(date)

            return@map event
        }
        return@map null
    }.filterNotNull()

    return  (oneOffEvents + reoccurringEvents).sortedBy { it.dateStart.value }
}

}