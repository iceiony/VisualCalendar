package com.iceiony.visualcalendar.preview

import biweekly.component.VEvent
import com.iceiony.visualcalendar.providers.DataProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.ReplaySubject
import java.time.LocalDateTime
import java.time.ZoneId.systemDefault
import java.util.Date

class TestDataProvider(
    private var testEvents: List<List<VEvent>>
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

    private var idx = -1
    private val subject = ReplaySubject.create<List<VEvent>>(1)

    init {
        idx++
        subject.onNext(testEvents.getOrNull(idx) ?: emptyList())
    }

    fun publish_next() {
        idx++
        subject.onNext(testEvents.getOrNull(idx) ?: emptyList())
    }

    override fun today(): Observable<List<VEvent>> {
        return subject.hide();
    }

    override fun refresh() {
        subject.onNext(testEvents.getOrNull(idx) ?: emptyList())
    }
}

