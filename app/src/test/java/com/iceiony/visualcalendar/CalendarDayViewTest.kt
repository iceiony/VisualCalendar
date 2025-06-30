package com.iceiony.visualcalendar

import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iceiony.visualcalendar.preview.TestDataProvider
import com.iceiony.visualcalendar.preview.TestTimeProvider
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.S])
class CalendarDayViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() { }

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
        val timeProvider = TestTimeProvider(
            now = LocalDateTime.of(2025, 6, 26, 7, 10)
        )

        val dataProvider = TestDataProvider(
            listOf(
                listOf(
                    TestDataProvider.calendarEvent(
                        "Test Event 1",
                        LocalDateTime.of(2025, 6, 26, 8, 0),
                        LocalDateTime.of(2025, 6, 26, 9, 0)
                    ),
                    TestDataProvider.calendarEvent(
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

}

