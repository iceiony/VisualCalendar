package com.iceiony.visualcalendar.providers

import android.annotation.SuppressLint
import biweekly.Biweekly
import biweekly.component.VEvent
import com.iceiony.visualcalendar.BuildConfig
import com.iceiony.visualcalendar.SystemTimeProvider
import com.iceiony.visualcalendar.TimeProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.ReplaySubject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

@SuppressLint("CheckResult")
class iCalDataProvider(
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val scheduler : Scheduler = Schedulers.io(),
    private val iCalUrl: String = BuildConfig.ICAL_DEBUG_URL
) : DataProvider {
    private val subject = ReplaySubject.create<List<VEvent>>(1)
    private val client = OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(30))
        .build()

    init {
        if (iCalUrl.isBlank()) {
            throw IllegalStateException("iCalUrl is not configured")
        }

        Observable
            .interval(0, 1, TimeUnit.HOURS, scheduler)
            .map { timeProvider.now().toLocalDate().atStartOfDay() }
            .distinctUntilChanged()
            .filter { today ->
                if (!subject.hasValue()) return@filter true

                val firstEventOfDay = subject.value?.firstOrNull() ?: return@filter true
                val firstEventStart = (firstEventOfDay as VEvent)
                    .dateStart.value.toInstant()
                    .atZone(ZoneOffset.systemDefault())
                    .toLocalDate()?.atStartOfDay()

                today.isAfter(firstEventStart)
            }
            .map { now -> getTodaysEvents(now) }
            .subscribeOn(scheduler)
            .subscribe(
                { events -> subject.onNext(events) },
                { error  -> subject.onError(error) }
            )
    }

    fun getTodaysEvents(now: LocalDateTime): List<VEvent> {
        val request = Request.Builder().url(iCalUrl).get().build()
        val response = client.newCall(request).execute()

        val body = response.body?.string() ?: throw IOException("Empty body")
        val calendar = Biweekly.parse(body).first()           // parse to ICalendar
            ?: throw IllegalStateException("Invalid ICS feed")

        val today = now.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC)
        val tomorrow = now.toLocalDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)

        return calendar.events.filter() { event ->
            event.dateStart.value.toInstant().isAfter(today) &&
            event.dateStart.value.toInstant().isBefore(tomorrow)
        }
    }

    override fun refresh() {
        val now = timeProvider.now().toLocalDate().atStartOfDay()
        Observable
            .fromCallable{ getTodaysEvents(now) }
            .subscribeOn(scheduler)
            .observeOn(scheduler)
            .subscribe(
                { events -> subject.onNext(events) },
                { error  -> subject.onError(error) }
            )
    }

    override fun today(): Observable<List<VEvent>> {
        return subject.hide()
    }
}