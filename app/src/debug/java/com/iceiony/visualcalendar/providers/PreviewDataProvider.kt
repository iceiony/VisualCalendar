package com.iceiony.visualcalendar.providers

import biweekly.component.VEvent
import biweekly.property.Attachment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId.systemDefault
import java.util.Date

class PreviewDataProvider(
    private var testEvents: List<List<VEvent>>,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) : DataProvider {

    companion object {
        fun calendarEvent(
            summary: String,
            startTime: LocalDateTime,
            endTime: LocalDateTime,
            attachments: List<ByteArray?> = emptyList(),
        ): VEvent {
            val event = VEvent()

            event.setSummary(summary)

            event.setDateStart(
                Date.from(startTime.atZone(systemDefault()).toInstant())
            )

            event.setDateEnd(
                Date.from(endTime.atZone(systemDefault()).toInstant())
            )

            for (content in attachments) {
                event.addAttachment(
                    Attachment(
                        "image/jpg",
                        content
                    )
                )
            }

            return event
        }
    }

    private var idx = -1
    private val subject = MutableSharedFlow<List<VEvent>>(replay = 1)

    init {
        scope.launch {
            publishNext()
        }
    }

    suspend fun publishNext() {
        idx++
        subject.emit(testEvents.getOrNull(idx) ?: emptyList())
    }

    override fun today(): SharedFlow<List<VEvent>> = subject

    override suspend fun refresh(now : LocalDateTime?) {
        subject.emit(testEvents.getOrNull(idx) ?: emptyList())
    }

    override suspend fun calendars(): Map<String, String> {
        return mapOf(
            "calendar_1" to "Calendar 1",
            "calendar_2" to "Calendar 2",
            "calendar_3" to "Calendar 3"
        )
    }

    override suspend fun getMainCalendar(): String {
        return "calendar_1"
    }

    override fun setMainCalendar(calendarId: String) {
        // No-op for preview
    }

    override fun destroy() { }
}

