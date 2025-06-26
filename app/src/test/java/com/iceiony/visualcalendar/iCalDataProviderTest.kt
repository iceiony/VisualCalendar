package com.iceiony.visualcalendar

import android.os.Build
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class iCalDataProviderTest {

    @Before
    fun setup() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun test_can_get_todays_events_successfully() {
        val now = java.time.LocalDateTime.of(2025, 6, 26, 7, 10)
        val dayStart = java.time.LocalDate.of(2025, 6, 26).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        val dayEnd   = java.time.LocalDate.of(2025, 6, 27).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()

        val events = iCalDataProvider.today(now = now)

        assert(events.isNotEmpty()) {
            "Expected to find events for today, but found none."
        }

        assert(events.all { event ->
            val start = event.dateStart.value.toInstant()
            start.isAfter(dayStart) && start.isBefore(dayEnd)
        }) {
            "All events should be within today's date range."
        }

    }
}