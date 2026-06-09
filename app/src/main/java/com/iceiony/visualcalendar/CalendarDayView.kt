package com.iceiony.visualcalendar

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.iceiony.visualcalendar.providers.DataProvider
import com.iceiony.visualcalendar.providers.SystemTimeProvider
import com.iceiony.visualcalendar.providers.TimeProvider
import com.iceiony.visualcalendar.providers.toTime
import kotlinx.coroutines.flow.map

@Composable
fun CalendarDayView(
    dataProvider: DataProvider,
    modifier: Modifier = Modifier,
    timeProvider: TimeProvider = SystemTimeProvider()
) {
    val events by dataProvider.today().collectAsState(emptyList())

    val now by remember {
        dataProvider.today().map { timeProvider.now() }
    }.collectAsState(initial = timeProvider.now())

    val title = if(now.hour < 18) {
        "It's " + now.toLocalDate().dayOfWeek.name
    } else {
        val tomorrow =  now.toLocalDate().atStartOfDay().plusDays(1)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(
                            color = colorResource(id = R.color.white).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val attachment = event.attachments.firstOrNull()
                    if (attachment != null) {
                        val bitmap = remember(attachment) {
                            BitmapFactory.decodeByteArray(attachment.data, 0, attachment.data.size)
                        }
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "event image attachment",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                                .widthIn(max = 43.dp)
                                //.background(Color.Blue)
                                .padding(vertical = 3.dp)
                                .padding(start = 5.dp, end = 0.dp )
                        )
                    }

                    Text(
                        text = event.summary.value,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp)
                            .padding(start = 8.dp)
                    )

                    Text(
                        text = "(${event.dateStart.toTime()} - ${event.dateEnd.toTime()})",
                        color = colorResource(id = R.color.gray_700),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
    }

}
