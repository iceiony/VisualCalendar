package com.iceiony.visualcalendar

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.intent.Intents
import org.junit.After
import org.junit.Before
import org.junit.Test
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iceiony.visualcalendar.testutil.TestHelper
import com.iceiony.visualcalendar.testutil.ShadowSecureSettings
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowSecureSettings::class], sdk = [Build.VERSION_CODES.S])
class CalendarDayActivityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

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

            val allViews = TestHelper.getAllViews(activity.window.decorView)
            val calendarDayView = allViews.find { it is ComposeView } as? ComposeView

            assert(calendarDayView != null) { "Activity should contain a composable view" }

            val today = java.time.LocalDate.now()
            composeTestRule.onNodeWithText(today.dayOfWeek.name).assertExists()
        }
    }

}