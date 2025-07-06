package com.iceiony.visualcalendar

import android.util.Log
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    DisposableEffect(context) {
        val screenOnReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_ON) {
                    dataProvider.refresh()
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        context.registerReceiver(screenOnReceiver, filter)

        onDispose {
            context.unregisterReceiver(screenOnReceiver)
        }
    }

    val eventsFlow = remember(dataProvider) {
        dataProvider.today().asFlow()
    }

    val events by eventsFlow.collectAsState(initial = emptyList())

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colorResource(id = R.color.teal_200).copy(alpha = 0.9f),
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