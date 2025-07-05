package com.iceiony.visualcalendar.preview

import biweekly.component.VEvent
import com.iceiony.visualcalendar.providers.DataProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.ReplaySubject
import java.time.LocalDateTime
import java.time.ZoneId.systemDefault
import java.util.Date

class TestDataProvider(
    private val testEvents: List<List<VEvent>>
) : DataProvider {

    companion object {
        fun calendarEvent(
            summary: String,
            startTime: LocalDateTime,
            endTime: LocalDateTime
        ): VEvent {
            val event = VEvent()

            event.setSummary(summary)

            event.setDateStart(
                Date.from(startTime.atZone(systemDefault()).toInstant())
            )
            event.setDateEnd(
                Date.from(endTime.atZone(systemDefault()).toInstant())
            )

            return event
        }
    }

    private val subject = ReplaySubject.create<List<VEvent>>(1)

    init {
        subject.onNext(testEvents.firstOrNull() ?: emptyList())
        testEvents.drop(1)
    }

    fun publish_next() {
        subject.onNext(testEvents.firstOrNull() ?: emptyList())
        testEvents.drop(1)
    }

    override fun today(): Observable<List<VEvent>> {
        return subject.hide();
    }

    override fun refresh() {
        subject.onNext(testEvents.firstOrNull() ?: emptyList())
    }
}

