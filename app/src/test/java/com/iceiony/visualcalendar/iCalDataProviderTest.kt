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
            now = LocalDateTime.of(2025, 6, 26, 7, 10),
            scheduler = testScheduler,
            context = context
        )

        val dataProvider = iCalDataProvider(timeProvider, testScheduler)

        timeProvider.advanceTimeBy(0)

        var events = dataProvider.today(context).test()

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
    }

    @Test
    fun `can get tomorrow's events after 6pm`() {
        val testScheduler = TestScheduler()
        val timeProvider = TestTimeProvider(
            now = LocalDateTime.of(2025, 6, 26, 6, 30), //i.e. 6:30 AM
            scheduler = testScheduler,
            context = context
        )

        val dataProvider = iCalDataProvider(timeProvider, testScheduler)

        timeProvider.advanceTimeBy(0)

        var events = dataProvider.today(context).test()

        events.values().clear()

        timeProvider.advanceTimeBy( 60 * 60 * 11) // 17:30:00
        assert(events.values().isEmpty()) {
            "Expected next days' events to not have been published yet."
        }

        timeProvider.advanceTimeBy( 60 * 30 + 1 ) // 18:00:01
        assert(events.values().isNotEmpty()) {
            "Expected to find next day's events but found none."
        }

        val dayStart = java.time.LocalDate.of(2025, 6, 27).atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()
        val dayEnd   = java.time.LocalDate.of(2025, 6, 28).atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()

        assert(events.values().first().all { event ->
            val start = event.dateStart.value.toInstant()
            start.isAfter(dayStart) && start.isBefore(dayEnd)
        }) {
            "All events should be within next day's date range."
        }


    }
}
