package com.iceiony.visualcalendar

import android.content.Context
import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iceiony.visualcalendar.preview.PreviewDataProvider
import com.iceiony.visualcalendar.preview.PreviewTimeProvider
import com.iceiony.visualcalendar.testutil.TestTimeProvider
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import android.util.Log
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.S])
class CalendarDayViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private lateinit var context: Context

    @Before
    fun setup() {
        //configure work manager since view data refresh depends on it
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
    fun test_initializes_successfully_for_todays_date() {
        composeTestRule.setContent { CalendarDayView() }

        val today = java.time.LocalDate.now()
        composeTestRule.onNodeWithText("It's ${today.dayOfWeek.name}").assertExists()
    }

    @Test
    fun test_shows_days_events(){
        val timeProvider = PreviewTimeProvider(
            now = LocalDateTime.of(2025, 6, 26, 7, 10)
        )

        val dataProvider = PreviewDataProvider(
            listOf(
                listOf(
                    PreviewDataProvider.calendarEvent(
                        "Test Event 1",
                        LocalDateTime.of(2025, 6, 26, 8, 0),
                        LocalDateTime.of(2025, 6, 26, 9, 0)
                    ),
                    PreviewDataProvider.calendarEvent(
                        "Test Event 2",
                        LocalDateTime.of(2025, 6, 26, 10, 0),
                        LocalDateTime.of(2025, 6, 26, 11, 0)
                    )
                )
            )
        )

        composeTestRule.setContent {
            CalendarDayView(
                dataProvider = dataProvider,
                timeProvider = timeProvider
            )
        }

        composeTestRule.onNodeWithText("Test Event 1").assertExists()
        composeTestRule.onNodeWithText("Test Event 2").assertExists()

        composeTestRule.onNodeWithText("(08:00 - 09:00)").assertExists()
        composeTestRule.onNodeWithText("(10:00 - 11:00)").assertExists()
    }

    @Test
    fun test_name_day_and_events_are_updated_when_day_changes() {
        val timeProvider = PreviewTimeProvider(
            now = LocalDateTime.of(2025, 6, 26, 7, 10),
        )

        val dataProvider = PreviewDataProvider(
            listOf(
                listOf(
                    PreviewDataProvider.calendarEvent(
                        "Test Day 1",
                        LocalDateTime.of(2025, 6, 26, 8, 0),
                        LocalDateTime.of(2025, 6, 26, 9, 0)
                    )
                ),
                listOf(
                    PreviewDataProvider.calendarEvent(
                        "Test Day 2",
                        LocalDateTime.of(2025, 6, 27, 7, 0),
                        LocalDateTime.of(2025, 6, 27, 12, 0)
                    )
                )
            )
        )

        composeTestRule.setContent {
            CalendarDayView(
                dataProvider = dataProvider,
                timeProvider = timeProvider
            )
        }

        composeTestRule.onNodeWithText("It's THURSDAY").assertExists()
        composeTestRule.onNodeWithText("Test Day 1").assertExists()
        composeTestRule.onNodeWithText("Test Day 2").assertDoesNotExist()

        timeProvider.advanceTimeBy(16 * 60 * 60 + 50 * 60 + 1)
        dataProvider.publish_next()

        composeTestRule.onNodeWithText("It's FRIDAY").assertExists()
        composeTestRule.onNodeWithText("Test Day 1").assertDoesNotExist()
        composeTestRule.onNodeWithText("Test Day 2").assertExists()
    }

}

