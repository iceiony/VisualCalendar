package com.iceiony.visualcalendar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iceiony.visualcalendar.preview.PreviewDataProvider
import com.iceiony.visualcalendar.preview.PreviewTimeProvider
import com.iceiony.visualcalendar.providers.DataProvider
import com.iceiony.visualcalendar.providers.iCalDataProvider
import com.iceiony.visualcalendar.providers.toTime
import kotlinx.coroutines.rx3.asFlow
import java.time.LocalDateTime
import kotlin.concurrent.timer

@Composable
fun CalendarDayView(
    modifier: Modifier = Modifier,
    dataProvider: DataProvider = iCalDataProvider(),
    timeProvider: TimeProvider = SystemTimeProvider()
) {
    val context = LocalContext.current

    val eventsFlow = remember(dataProvider) {
        dataProvider.today(context).asFlow()
    }

    val events by eventsFlow.collectAsState(initial = emptyList())

    val title = if(timeProvider.now().hour < 18) {
        val today = timeProvider.now().toLocalDate()
        "It's " + today.dayOfWeek.name
    } else {
        val tomorrow =  timeProvider.now().toLocalDate().atStartOfDay().plusDays(1)
        "Tomorrow is " + tomorrow.dayOfWeek.name
    }

    val colour = if ("Tomorrow" in title) R.color.title_orange else R.color.title_teal

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colorResource(id = colour).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Text(
                text = title,
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
                            color = colorResource(id = R.color.white).copy(alpha = 0.9f),
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