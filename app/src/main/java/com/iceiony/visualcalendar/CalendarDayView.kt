package com.iceiony.visualcalendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.iceiony.visualcalendar.providers.DataProvider
import com.iceiony.visualcalendar.providers.iCalDataProvider


@Composable
fun CalendarDayView(
    modifier: Modifier = Modifier,
    dataProvider: DataProvider = iCalDataProvider(),
    timeProvider: TimeProvider = SystemTimeProvider()
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text =  timeProvider.now().toLocalDate().dayOfWeek.name)
    }

    LaunchedEffect(Unit) {
    }
}
@Preview(showBackground = true)
@Composable
fun CalendarDayViewPreview() {
    CalendarDayView()
}