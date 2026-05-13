package com.iceiony.visualcalendar.providers.google

import android.content.Context
import biweekly.component.VEvent
import com.iceiony.visualcalendar.SystemTimeProvider
import com.iceiony.visualcalendar.TimeProvider
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import java.time.LocalDateTime
import com.iceiony.visualcalendar.VisualCalendarApp
import com.iceiony.visualcalendar.providers.ScheduledDataProvider

class GoogleCalendarDataProvider(
    context: Context = VisualCalendarApp.instance.applicationContext,
    timeProvider: TimeProvider = SystemTimeProvider(),
    scheduler : Scheduler = Schedulers.io(),
) : ScheduledDataProvider(timeProvider, scheduler) {

    private val authProvider: GoogleAuthProvider = GoogleAuthProvider(context)

    override fun getDaysEvents(now: LocalDateTime): List<VEvent> {
        //val token = authProvider.getValidAccessToken()
        TODO("Not yet implemented")
    }

}