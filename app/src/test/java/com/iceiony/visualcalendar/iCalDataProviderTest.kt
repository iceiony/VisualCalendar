package com.iceiony.visualcalendar

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.impl.schedulers
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.iceiony.visualcalendar.providers.iCalDataProvider
import com.iceiony.visualcalendar.testutil.TestTimeProvider
import io.reactivex.rxjava3.schedulers.TestScheduler
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit


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
    fun `can subscribe to calendar events`() = runTest {
        val dataProvider = iCalDataProvider(context)

        dataProvider.today().test {
            assert(awaitItem() == null) {
                "Expected initial value to be null before first refresh, but got non-null value."
            }

            val events = awaitItem() ?: throw AssertionError("Expected to receive list of events, but got null.")

            println("Events for today:")
            events.forEach { event ->
                println("- ${event.summary.value} at ${event.dateStart.value} - ${event.dateEnd.value}")
            }
        }

    }

    @Test
    fun `can get today's events before 6pm`()  = runTest{
        val timeProvider = TestTimeProvider(
            now = LocalDateTime.of(2026, 2, 21, 7, 10),
            context = context, scheduler = testScheduler
        )

        val dataProvider = iCalDataProvider(context, timeProvider)
        dataProvider.today().test {
            assert(awaitItem() == null) {
                "Expected initial value to be null before first refresh, but got non-null value."
            }

            val events = awaitItem()
                ?: throw AssertionError("Expected to receive list of events, but got null.")

            assert(events.isNotEmpty()) {
                "Expected to have published today's events, but nothing was published."
            }

            val dayStart = java.time.LocalDate.of(2026, 2, 21)
                .atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()
            val dayEnd = java.time.LocalDate.of(2026, 2, 22)
                .atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()

            assert(events.all { event ->
                val start = event.dateStart.value.toInstant()
                start.isAfter(dayStart) && start.isBefore(dayEnd)
            }) {
                "All events should be within today's date range."
            }
        }
    }

    @Test
    fun `can get tomorrow's events after 6pm`() = runTest{
        val timeProvider = TestTimeProvider(
            now = LocalDateTime.of(2026, 2, 20, 17, 30),
            context = context, scheduler = testScheduler
        )

        val dataProvider = iCalDataProvider(context, timeProvider)

        timeProvider.advanceTimeBy(0)

        dataProvider.today().test {
            assert(awaitItem() == null) {
                "Expected initial value to be null before first refresh, but got non-null value."
            }

            var events = awaitItem()
                ?: throw AssertionError("Expected to receive list of events, but got null.")

            assert(events.isEmpty()) {
                "Not expecting any events for the day. Test not setup correctly."
            }

            timeProvider.advanceTimeBy(60 * 30 + 1) // 18:00:01

            events = awaitItem()
                ?: throw AssertionError("Expected to receive list of events, but got null.")

            assert(events.isNotEmpty()) {
                "Expected next days' events to have been published."
            }

            val dayStart = java.time.LocalDate.of(2026, 2, 21)
                .atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()
            val dayEnd = java.time.LocalDate.of(2026, 2, 22)
                .atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()

            assert(events.all { event ->
                val start = event.dateStart.value.toInstant()
                start.isAfter(dayStart) && start.isBefore(dayEnd)
            }) {
                "All events should be within next day's date range."
            }
        }
    }

    @Test
    fun `the event list is refreshed in the morning`() = runTest {
        val timeProvider = TestTimeProvider(
            now = LocalDateTime.of(2026, 2, 20, 18, 1),
            context = context, scheduler = testScheduler
        )

        val dataProvider = iCalDataProvider(context, timeProvider)

        val dayStart = java.time.LocalDate.of(2026, 2, 21).atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()
        val dayEnd   = java.time.LocalDate.of(2026, 2, 22).atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()

        turbineScope {
            val eventsSource = dataProvider.today().testIn(this)
            val timeSource = dataProvider.today()
                .map({ timeProvider.now() })
                .testIn(this)

            val initialEvents = eventsSource.awaitItem()
            val initialTime = timeSource.awaitItem()

            assert(initialEvents == null) {
                "Expected initial events to be null before first refresh, but got $initialEvents."
            }

            val eventsYesterday = eventsSource.awaitItem()
                ?: throw AssertionError("Expected to receive list of events, but got null.")
            val timeYesterday = timeSource.awaitItem()

            assert(eventsYesterday.isNotEmpty()) {
                "Expected to find next day's events but found none."
            }

            assert(eventsYesterday.all { event ->
                val start = event.dateStart.value.toInstant()
                start.isAfter(dayStart) && start.isBefore(dayEnd)
            }) {
                "All events should be within next day's date range."
            }

            timeProvider.advanceTimeTo(
                LocalDateTime.of(2026, 2, 21, 6, 0, 1)
            )

            val eventsToday = eventsSource.awaitItem()
                ?: throw AssertionError("Expected to receive list of events, but got null.")
            val timeToday = timeSource.awaitItem()

            assert(timeToday != timeYesterday) {
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

            eventsSource.cancel()
            timeSource.cancel()
        }
    }

    @Test
    fun `re-occurring calendar events show up`() = runTest {
        val timeProvider = TestTimeProvider(
            now = LocalDateTime.of(2026, 2, 23, 19, 7),
            context = context, scheduler = testScheduler
        )

        val dataProvider = iCalDataProvider(context, timeProvider)

        dataProvider.today().test {
            assert(awaitItem() == null) {
                "Expected initial value to be null before first refresh, but got non-null value."
            }

            val events = awaitItem()
                ?: throw AssertionError("Expected to receive list of events, but got null.")

            assert(events.isNotEmpty()) {
                "Expected next days' events to have been published."
            }

            val dayStart = java.time.LocalDate.of(2026, 2, 24)
                .atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()
            val dayEnd = java.time.LocalDate.of(2026, 2, 25)
                .atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()

            assert(events.all { event ->
                val start = event.dateStart.value.toInstant()
                start.isAfter(dayStart) && start.isBefore(dayEnd)
            }) {
                "All events should be within next day's date range."
            }
        }
    }
}
