package com.iceiony.visualcalendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    dataProvider.refresh()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val eventsFlow = remember(dataProvider) {
        dataProvider.today().asFlow()
    }



    val events by eventsFlow.collectAsState(initial = emptyList())

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colorResource(id = R.color.teal_200),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Text(
                text = "It's " + timeProvider.now().toLocalDate().dayOfWeek.name,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(vertical = 2.dp, horizontal = 16.dp)
            )
        }


        events.forEach { event ->

            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = colorResource(id = R.color.white),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        text = event.summary.value,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .padding(vertical = 2.dp, horizontal = 16.dp)
                            .align(Alignment.CenterStart),
                    )

                    Text(
                        text = "(${event.dateStart.toTime()} - ${event.dateEnd.toTime()})",
                        color = colorResource(id = R.color.gray_700),
                        modifier = Modifier.padding(end = 8.dp).align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
    }
}

@Preview()
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