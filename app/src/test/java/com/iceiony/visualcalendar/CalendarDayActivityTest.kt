package com.iceiony.visualcalendar

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.intent.Intents
import org.junit.After
import org.junit.Before
import org.junit.Test
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowSecureSettings::class], sdk = [Build.VERSION_CODES.S])
class CalendarDayActivityTest {
    @Before
    fun setup() {
        Intents.init()

        //assume overlay permissions granted
        ShadowSettings.setCanDrawOverlays(true)

        //assume accessibility service enabled
        val context = ApplicationProvider.getApplicationContext<Context>()
        val serviceId = "${context.packageName}/com.iceiony.CalendarAccessibilityService"
        ShadowSecureSettings.setString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            serviceId)
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    private fun getAllViews(root: View): List<View> {
        val result = mutableListOf<View>()
        val queue = ArrayDeque<View>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val view = queue.removeFirst()
            result.add(view)
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    queue.add(view.getChildAt(i))
                }
            }
        }
        return result
    }

    @Test
    fun test_launches_onboarding_activity_when_permissions_are_missing(){
        ShadowSettings.setCanDrawOverlays(false)
        ShadowSettings.reset()

        ActivityScenario.launch(CalendarDayActivity::class.java)

        // Verify that the OnboardingActivity was launched
        intended(hasComponent(CalendarDayActivity::class.java.name))
    }

    @Test
    fun test_inflates_calendar_day_view_with_data_for_current_day() {
        val calendar = ActivityScenario.launch(CalendarDayActivity::class.java)

        calendar.onActivity { activity ->

            val allViews = getAllViews(activity.window.decorView)
            val calendarView = allViews.find { it is CalendarDayView } as? CalendarDayView

            assert(calendarView != null) { "Activity should display the CalendarDayView" }
        }
    }

}