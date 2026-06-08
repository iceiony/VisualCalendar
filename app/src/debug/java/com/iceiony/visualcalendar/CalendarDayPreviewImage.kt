package com.iceiony.visualcalendar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.iceiony.visualcalendar.providers.PreviewDataProvider
import com.iceiony.visualcalendar.providers.PreviewTimeProvider
import com.iceiony.visualcalendar.providers.google.GoogleCalendarDataProvider.Companion.fetchGoogleDriveImageData
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

private val previewImageBytes: ByteArray by lazy {
    val size = 24
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()

    // Checkerboard pattern
    val tile = 6
    for (row in 0 until size / tile) {
        for (col in 0 until size / tile) {
            paint.color = if ((row + col) % 2 == 0) Color.WHITE else Color.parseColor("#4A90D9")
            canvas.drawRect(
                (col * tile).toFloat(), (row * tile).toFloat(),
                ((col + 1) * tile).toFloat(), ((row + 1) * tile).toFloat(),
                paint
            )
        }
    }

    ByteArrayOutputStream().use { stream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        bitmap.recycle()
        stream.toByteArray()
    }
}

@Preview()
@Composable
fun CalendarDayViewPreviewImage() {
    val timeProvider = PreviewTimeProvider(
        now = LocalDateTime.of(2025, 6, 26, 19, 10)
    )

    val dataProvider = PreviewDataProvider(
        listOf(
            listOf(
                PreviewDataProvider.calendarEvent(
                    "Test Event 1",
                    LocalDateTime.of(2025, 6, 26, 8, 0),
                    LocalDateTime.of(2025, 6, 26, 9, 0),
                    attachments = listOf(previewImageBytes)
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
