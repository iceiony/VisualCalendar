package com.iceiony.visualcalendar

import android.os.Build
import com.iceiony.visualcalendar.testutil.TestTimeProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class iCalDataProviderTest {
    @Before
    fun setup() { }

    @After
    fun tearDown() { }

    @Test
    fun test_can_subscribe_to_todays_events_successfully() {
        val timeProvider  = TestTimeProvider(
            now = LocalDateTime.of(2025, 6, 26, 7, 10)
        )

        val dataProvider = iCalDataProvider(timeProvider, timeProvider.testScheduler)
        timeProvider.testScheduler.triggerActions()

        var events = dataProvider.today().test()

        assert(events.values().isNotEmpty()) {
            "Expected to find events for today, but found none."
        }

        var dayStart = java.time.LocalDate.of(2025, 6, 26).atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()
        var dayEnd   = java.time.LocalDate.of(2025, 6, 27).atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()

        assert(events.values().first().all { event ->
            val start = event.dateStart.value.toInstant()
            start.isAfter(dayStart) && start.isBefore(dayEnd)
        }) {
            "All events should be within today's date range."
        }

        events.values().clear()
        timeProvider.advanceTimeBy( 60 * 60 * 12) // Advance by half a day
        assert(events.values().isEmpty()) {
            "Expected next days' events to not have been published yet."
        }

        timeProvider.advanceTimeBy( 60 * 60 * 12 + 10) // Advance by half a day
        assert(events.values().isNotEmpty()) {
            "Expected to find next day's events but found none."
        }

        dayStart = java.time.LocalDate.of(2025, 6, 27).atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()
        dayEnd   = java.time.LocalDate.of(2025, 6, 28).atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()

        assert(events.values().first().all { event ->
            val start = event.dateStart.value.toInstant()
            start.isAfter(dayStart) && start.isBefore(dayEnd)
        }) {
            "All events should be within next day's date range."
        }


    }
}