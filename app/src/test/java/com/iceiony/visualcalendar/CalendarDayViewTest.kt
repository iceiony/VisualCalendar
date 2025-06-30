package com.iceiony.visualcalendar

import android.os.Build
import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.iceiony.visualcalendar.testutil.TestHelper
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Test
import org.robolectric.Robolectric

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class CalendarDayViewTest {
    @Before
    fun setup() { }

    @After
    fun tearDown() { }

    @Test
    fun test_initializes_successfully_for_todays_date() {
        val calendarDayView = CalendarDayView(ApplicationProvider.getApplicationContext<Context>())
        assert(calendarDayView != null) {
            "CalendarDayView should initialize successfully"
        }

        //check it contains TextView for today's day name (in any View)
        val today = java.time.LocalDate.now()
        val dayNameTextView = TestHelper
            .getAllViews(calendarDayView)
            .find{ it is TextView && today.dayOfWeek.name == (it as TextView).text }

        assert(dayNameTextView != null) {
            "CalendarDayView should contain TextView for today's day name"
        }
    }

}
