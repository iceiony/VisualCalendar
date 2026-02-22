package com.iceiony.visualcalendar

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.iceiony.visualcalendar.providers.iCalDataProvider
import com.iceiony.visualcalendar.testutil.TestTimeProvider
import io.reactivex.rxjava3.schedulers.TestScheduler
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
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build();

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);
    }

    @After
    fun tearDown() { }

    @Test
    fun `can subscribe to today's events successfully`() {
        val testScheduler = TestScheduler()
        val timeProvider = TestTimeProvider(
            now = LocalDateTime.of(2026, 2, 21, 7, 10),
            scheduler = testScheduler,
            context = context
        )

        val dataProvider = iCalDataProvider(timeProvider, testScheduler)

        timeProvider.advanceTimeBy(0)

        val events = dataProvider.today(context).test()

        assert(events.values().isNotEmpty()) {
            "Expected to have published today's events, but nothing was published."
        }

        assert(events.values().last().isNotEmpty()) {
            "Expected to find some events for the day, but found none."
        }

        val dayStart = java.time.LocalDate.of(2026, 2, 21).atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()
        val dayEnd   = java.time.LocalDate.of(2026, 2, 22).atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()

        assert(events.values().first().all { event ->
            val start = event.dateStart.value.toInstant()
            start.isAfter(dayStart) && start.isBefore(dayEnd)
        }) {
            "All events should be within today's date range."
        }
    }

    @Test
    fun `will get tomorrow's events after 6pm`() {
        val testScheduler = TestScheduler()
        val timeProvider = TestTimeProvider(
            now = LocalDateTime.of(2026, 2, 20, 17, 30),
            scheduler = testScheduler,
            context = context
        )

        val dataProvider = iCalDataProvider(timeProvider, testScheduler)

        timeProvider.advanceTimeBy(0)

        val events = dataProvider.today(context).test()

        assert(events.values().last().isEmpty()) {
            "Not expecting any events for the day. Test not setup correctly."
        }

        timeProvider.advanceTimeBy( 60 * 30 + 1) // 18:00:01

        assert(events.values().last().isNotEmpty()) {
            "Expected next days' events to have been published."
        }

        val dayStart = java.time.LocalDate.of(2026, 2, 21).atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()
        val dayEnd   = java.time.LocalDate.of(2026, 2, 22).atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()

        assert(events.values().last().all { event ->
            val start = event.dateStart.value.toInstant()
            start.isAfter(dayStart) && start.isBefore(dayEnd)
        }) {
            "All events should be within next day's date range."
        }
    }

    @Test
    fun `the event list is refreshed in the morning`() {
        val testScheduler = TestScheduler()
        val timeProvider = TestTimeProvider(
            now = LocalDateTime.of(2026, 2, 20, 18, 1),
            scheduler = testScheduler,
            context = context
        )

        val dataProvider = iCalDataProvider(timeProvider, testScheduler)

        timeProvider.advanceTimeBy(0)

        val eventsSource = dataProvider.today(context)

        val events = eventsSource.test()
        val eventTime = eventsSource.map { timeProvider.now() }.test()

        assert(events.values().isNotEmpty()) {
            "Expected to have events published, but nothing was published."
        }

        val eventsYesterday = events.values().last()
        val timeYesterday   = eventTime.values().last()

        assert(eventsYesterday.isNotEmpty()) {
            "Expected to find next day's events but found none."
        }

        val dayStart = java.time.LocalDate.of(2026, 2, 21).atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()
        val dayEnd   = java.time.LocalDate.of(2026, 2, 22).atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()

        assert(eventsYesterday.all { event ->
            val start = event.dateStart.value.toInstant()
            start.isAfter(dayStart) && start.isBefore(dayEnd)
        }) {
            "All events should be within next day's date range."
        }

        timeProvider.advanceTimeTo(
            LocalDateTime.of(2026, 2, 21, 6, 0, 1)
        )

        val eventsToday = events.values().last()
        val timeToday   = eventTime.values().last()

        assert( timeToday != timeYesterday) {
            "Expected time to have advanced."
        }

        assert(eventsYesterday.size == eventsToday.size) {
            "Expected to have same number of events after refresh, but had ${eventsYesterday.size} before and ${eventsToday.size} after."
        }

        assert(eventsToday.all { event ->
            val start = event.dateStart.value.toInstant()
            start.isAfter(dayStart) && start.isBefore(dayEnd)
        }) {
            "All events should still be within next day's date range."
        }

    }
}
