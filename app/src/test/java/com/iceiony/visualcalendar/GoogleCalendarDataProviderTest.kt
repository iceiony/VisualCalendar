package com.iceiony.visualcalendar

import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.iceiony.visualcalendar.providers.google.GoogleAuthProvider
import com.iceiony.visualcalendar.providers.google.GoogleCalendarDataProvider
import com.iceiony.visualcalendar.testutil.TestTimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import kotlin.intArrayOf


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
@OptIn(ExperimentalCoroutinesApi::class)
class GoogleCalendarDataProviderTest {
    private lateinit var context: Context

    companion object {
        var authProvider: GoogleAuthProvider? = null
    }

    @Before
    fun setup()  = runTest {
        Dispatchers.setMain(StandardTestDispatcher())

        context = ApplicationProvider.getApplicationContext<Context>()

        WorkManagerTestInitHelper.initializeTestWorkManager(context);

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )

        if(authProvider == null || !authProvider!!.isAuthorised()) {
            authProvider = GoogleAuthProvider(context)
            authProvider?.setAuthState(
                """
                {
                  "access_token" : "${BuildConfig.TEST_ACCESS_TOKEN}" ,
                  "expires_in" : ${- 60} ,
                  "refresh_token" : "${BuildConfig.TEST_REFRESH_TOKEN}" ,
                  "scope" : "https://www.googleapis.com/auth/calendar.readonly",
                  "token_type" : "Bearer"
                }
            """.trimIndent().let { JSONObject(it) }
            )

            assert(authProvider!!.isAuthorised() ) {
                "Test setup failed: Can't authorise GoogleCalendar access with provided test credentials."
            }
        }
    }

    @After
    fun tearDown() {
        WorkManager.getInstance(context).cancelAllWork()
        WorkManagerTestInitHelper.closeWorkDatabase()
        Dispatchers.resetMain()
    }

    @Test
    fun `can retrieve the list of calendars the user has access to`() = runTest {
        val dataProvider = GoogleCalendarDataProvider(context, authProvider = authProvider!!)

        val calendars = dataProvider.calendars()

        assert(calendars.isNotEmpty()) {
            "Expected to retrieve at least one calendar, but got none."
        }

        //expect more than one
        assert(calendars.size > 1) {
            "Expected to retrieve more than one calendar, but got ${calendars.size}."
        }
    }

    @Test
    fun `defaults to the first calendar when no calendar main is configured`() = runTest {
        val dataProvider = GoogleCalendarDataProvider(context, authProvider = authProvider!!)

        val calendars = dataProvider.calendars()

        assert(calendars.size > 2) {
            "Test setup incorrect, authorisation should be an account with access to more than 1 calendar"
        }

        assert( dataProvider.getMainCalendar() == null || dataProvider.getMainCalendar()!!.isEmpty()) {
            "Expected no main calendar to be configured yet, but got ${dataProvider.getMainCalendar()}."
        }

        dataProvider.setMainCalendar(calendars.keys.last())

        assert( dataProvider.getMainCalendar() != null ) {
            "Expected selected calendar ID to have changed"
        }

        assert( calendars.keys.last() == dataProvider.getMainCalendar()) {
            "Expected selected calendar ID to be ${calendars.keys.last()}, but got ${dataProvider.getMainCalendar()}."
        }

    }

    @Test
    fun `can subscribe to calendar events`()  = runTest {
        val dataProvider = GoogleCalendarDataProvider( context, authProvider = authProvider!! )

        dataProvider.today().test {
            val events = awaitItem()

            println("Events for today:")
            events.forEach { event ->
                println("- ${event.summary.value} at ${event.dateStart.value} - ${event.dateEnd.value}")
            }
        }
    }

    @Test
    fun `can get today's events before 6pm`()  = runTest {
        val timeProvider = TestTimeProvider(
            now = LocalDateTime.of(2026, 2, 21, 7, 10),
            context = context, scheduler = testScheduler
        )

        val dataProvider = GoogleCalendarDataProvider(context, timeProvider, authProvider!!)

        setMainCalendar(dataProvider)

        dataProvider.today().test {
            val events = awaitItem()

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

    private suspend fun setMainCalendar(dataProvider: GoogleCalendarDataProvider, keyword: String = "Teo") {
        val mainCalendar =
            dataProvider.calendars().filter { it.value.contains("Cornel") }.keys.firstOrNull()
                ?: throw AssertionError("Test setup incorrect: No calendar found with description containing 'Teo'")

        dataProvider.setMainCalendar(mainCalendar)
    }

    @Test
    fun `can get tomorrow's events after 6pm`()  = runTest{
        val timeProvider = TestTimeProvider(
            now = LocalDateTime.of(2026, 2, 20, 17, 30),
            context = context, scheduler = testScheduler
        )

        val dataProvider = GoogleCalendarDataProvider(context, timeProvider, authProvider!!)
        setMainCalendar(dataProvider)

        dataProvider.today().test {
            var events = awaitItem()

            assert(events.isEmpty()) {
                "Not expecting any events for the day. Test not setup correctly."
            }

            timeProvider.advanceTimeBy(60 * 30 + 1) // 18:00:01

            events = awaitItem()
                ?: throw AssertionError("Expected to receive list of events after time advanced, but got null.")

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
            context = context, scheduler = testScheduler,
        )

        val dataProvider = GoogleCalendarDataProvider(context, timeProvider, authProvider!!)
        setMainCalendar(dataProvider)

        val dayStart = java.time.LocalDate.of(2026, 2, 21)
            .atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()
        val dayEnd = java.time.LocalDate.of(2026, 2, 22)
            .atStartOfDay(java.time.ZoneOffset.systemDefault()).toInstant()

        turbineScope {
            val eventsSource = dataProvider.today().testIn(this)
            val timeSource = dataProvider.today()
                .map({ timeProvider.now() })
                .testIn(this)

            val eventsYesterday = eventsSource.awaitItem()

            val timeYesterday = timeSource.awaitItem()

            assert(eventsYesterday.isNotEmpty()) {
                "Expected to have events published, but nothing was published."
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

        val dataProvider = GoogleCalendarDataProvider(context, timeProvider, authProvider!!)
        setMainCalendar(dataProvider)

        dataProvider.today().test {
            val events = awaitItem()

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

    @Test
    fun `events with attached images have the image data included`() = runTest {
        val timeProvider = TestTimeProvider(
            now = LocalDateTime.of(2026, 6, 10, 6, 30),
            context = context, scheduler = testScheduler
        )

        val dataProvider = GoogleCalendarDataProvider(context, timeProvider, authProvider!!)
        setMainCalendar(dataProvider, "Cornel")

        dataProvider.today().test {
            val events = awaitItem()

            assert(events.isNotEmpty()) {
                "Expected next days' events to have been published."
            }

            assert(events.size == 1) {
                "Test setup incorrect: Expected exactly one event for the test date, but found ${events.size}."
            }

            val event = events.first()

            assert(event.attachments != null && event.attachments.size == 1) {
                "Expected at one event with attachments, but found none."
            }

             val attachment = event.attachments.first()

            assert(attachment.data != null && attachment.data.isNotEmpty()) {
                "Expected attachment to have non-empty data, but it was empty."
            }
        }
    }

}