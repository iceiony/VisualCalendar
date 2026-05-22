package com.iceiony.visualcalendar.preview

import android.content.Context
import biweekly.component.VEvent
import com.iceiony.visualcalendar.providers.DataProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.ReplaySubject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onCompletion
import java.time.LocalDateTime
import java.time.ZoneId.systemDefault
import java.util.Date

class PreviewDataProvider(
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
    private val subject: MutableStateFlow<List<VEvent>>

    init {
        idx++
        subject = MutableStateFlow<List<VEvent>>(
            testEvents.getOrNull(idx) ?: emptyList()
        )
    }

    suspend fun publishNext() {
        idx++
        subject.emit(testEvents.getOrNull(idx) ?: emptyList())
    }

    override fun today(): StateFlow<List<VEvent>> = subject

    override suspend fun refresh(now : LocalDateTime) {
        subject.emit(testEvents.getOrNull(idx) ?: emptyList())
    }

    override suspend fun calendars(): Map<String, String> {
        return mapOf(
            "calendar_1" to "Calendar 1",
            "calendar_2" to "Calendar 2",
            "calendar_3" to "Calendar 3"
        )
    }

    override suspend fun getMainCalendar(): String? {
        return "calendar_1"
    }

    override fun setMainCalendar(calendarId: String) {
        // No-op for preview
    }

    override fun destroy() { }
}

