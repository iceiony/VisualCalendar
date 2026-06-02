package com.iceiony.visualcalendar

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.iceiony.visualcalendar.providers.PreviewDataProvider
import com.iceiony.visualcalendar.providers.PreviewTimeProvider
import java.time.LocalDateTime

@Preview()
@Composable
fun CalendarDayViewPreview() {
    val timeProvider = PreviewTimeProvider(
        now = LocalDateTime.of(2025, 6, 26, 19, 10)
    )

    val dataProvider = PreviewDataProvider(
        listOf(
            listOf(
                PreviewDataProvider.calendarEvent(
                    "Test Event 1",
                    LocalDateTime.of(2025, 6, 26, 8, 0),
                    LocalDateTime.of(2025, 6, 26, 9, 0)
                ),
                PreviewDataProvider.calendarEvent(
                    "Test Event 2 👌",
                    LocalDateTime.of(2025, 6, 26, 10, 0),
                    LocalDateTime.of(2025, 6, 26, 11, 0)
                )
            )
        )
    )

    CalendarDayView(
        dataProvider = dataProvider,
        timeProvider = timeProvider
    )
}
