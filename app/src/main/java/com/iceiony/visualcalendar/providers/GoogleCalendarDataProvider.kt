package com.iceiony.visualcalendar.providers

import android.content.Context
import android.util.Log
import biweekly.component.VEvent
import com.iceiony.visualcalendar.SystemTimeProvider
import com.iceiony.visualcalendar.TimeProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.ReplaySubject
import okhttp3.OkHttpClient
import java.time.Duration
import java.time.LocalDateTime

class GoogleCalendarDataProvider(
    timeProvider: TimeProvider = SystemTimeProvider(),
    scheduler : Scheduler = Schedulers.io(),
) : ScheduledDataProvider(timeProvider, scheduler) {

    override fun getDaysEvents(now: LocalDateTime): List<VEvent> {
        TODO("Not yet implemented")
    }

}