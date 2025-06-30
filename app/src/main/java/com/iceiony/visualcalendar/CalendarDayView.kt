package com.iceiony.visualcalendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iceiony.visualcalendar.preview.TestDataProvider
import com.iceiony.visualcalendar.preview.TestTimeProvider
import com.iceiony.visualcalendar.providers.DataProvider
import com.iceiony.visualcalendar.providers.iCalDataProvider
import com.iceiony.visualcalendar.providers.toTime
import kotlinx.coroutines.rx3.asFlow
import java.time.LocalDateTime

@Composable
fun CalendarDayView(
    modifier: Modifier = Modifier,
    dataProvider: DataProvider = iCalDataProvider(),
    timeProvider: TimeProvider = SystemTimeProvider()
) {
    val eventsFlow = remember(dataProvider) {
        dataProvider.today().asFlow()
    }

    val events by eventsFlow.collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "It's " + timeProvider.now().toLocalDate().dayOfWeek.name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        events.forEach { event ->
            Row() {
                Text(
                    text = event.summary.value,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Text(
                    text = "(${event.dateStart.toTime()} - ${event.dateEnd.toTime()})",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
    }
}

@Preview(
    showBackground = true,
    device = "spec:parent=Nexus 7 2013,orientation=landscape"
)
@Composable
fun CalendarDayViewPreview() {
    val timeProvider = TestTimeProvider(
        now = LocalDateTime.of(2025, 6, 26, 7, 10)
    )

    val dataProvider = TestDataProvider(
        listOf(
            listOf(
                TestDataProvider.calendarEvent(
                    "Test Event 1",
                    LocalDateTime.of(2025, 6, 26, 8, 0),
                    LocalDateTime.of(2025, 6, 26, 9, 0)
                ),
                TestDataProvider.calendarEvent(
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