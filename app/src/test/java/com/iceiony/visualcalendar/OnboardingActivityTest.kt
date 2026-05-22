package com.iceiony.visualcalendar

import android.app.Activity
import android.app.Application
import kotlinx.coroutines.test.runTest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.os.StrictMode
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.iceiony.visualcalendar.providers.google.GoogleAuthProvider
import com.iceiony.visualcalendar.providers.google.GoogleCalendarDataProvider
import com.iceiony.visualcalendar.testutil.ShadowSecureSettings
import com.iceiony.visualcalendar.viewmodels.PermissionsViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings
import org.robolectric.shadows.ShadowToast


@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowSecureSettings::class], sdk = [Build.VERSION_CODES.S])
class OnboardingActivityTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()
    private lateinit var application: Application

    @Before
    fun setup() {
        Intents.init()

        application = ApplicationProvider.getApplicationContext<Application>()

        WorkManagerTestInitHelper.initializeTestWorkManager(application)

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }

    @After
    fun tearDown() {
        WorkManager.getInstance(application).cancelAllWork()
        WorkManagerTestInitHelper.closeWorkDatabase()
        shadowOf(Looper.getMainLooper()).idle()
        Intents.release()
    }

    @Test
    fun `requests overlay permissions when not granted`() {
        ShadowSettings.setCanDrawOverlays(false)

        ActivityScenario.launch(OnboardingActivity::class.java)

        val latestToastText = ShadowToast.getTextOfLatestToast()
        assert(latestToastText == "Please enable overlay permission for Visual Calendar") {
            "Expected toast message to be shown when overlay permission is not granted, but got: $latestToastText"
        }

        intended(hasAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        ShadowSettings.setCanDrawOverlays(true)
    }

    @Test
    fun `creates toast when overlay permission not granted`() {
        ShadowSettings.setCanDrawOverlays(false)
        ShadowSettings.reset()

        val scenario = ActivityScenario.launch(OnboardingActivity::class.java)

        scenario.onActivity { activity ->
            activity.viewModel.overlayPermissionsCallback.onActivityResult(
                ActivityResult(Activity.RESULT_CANCELED, null)
            )

            val latestToastText = ShadowToast.getTextOfLatestToast()
            assert( latestToastText == "Permission not granted to show overlay") {
                "Expected toast message to be shown when overlay permission is not granted, but got: $latestToastText"
            }
        }
    }

    @Test
    fun `requests accessibility when overlay already granted but not accessibility`() {
        ShadowSettings.setCanDrawOverlays(true)
        ActivityScenario.launch(OnboardingActivity::class.java)
        intended(hasAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION), Intents.times(0))

        intended(hasAction(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    @Test
    fun `does not open accessibility settings when already granted`() {
        ShadowSettings.setCanDrawOverlays(true)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val serviceId = "${context.packageName}/com.iceiony.CalendarAccessibilityService"

        ShadowSecureSettings.setString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            serviceId)

        ActivityScenario.launch(OnboardingActivity::class.java)

        intended(hasAction(Settings.ACTION_ACCESSIBILITY_SETTINGS), Intents.times(0))
    }

    @Test
    fun `waits for google authentication confirmation when no credentials available`()  = runTest {
        GoogleAuthProvider(application).clearAuthState()

        ShadowSettings.setCanDrawOverlays(true)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val serviceId = "${context.packageName}/com.iceiony.CalendarAccessibilityService"

        ShadowSecureSettings.setString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            serviceId)

        val scenario  = ActivityScenario.launchActivityForResult(OnboardingActivity::class.java)

        assert(scenario.state == Lifecycle.State.RESUMED) {
            "Expected OnboardingActivity to wait for user confirmation after permissions are granted, but it is not in RESUMED state"
        }
    }

    @Test
    fun `completes the OnboardingActivity when permissions are already granted`()  = runTest{
        GoogleAuthProvider(application).setAuthState(
            """
            {
                "access_token": "test_access_token",
                "refresh_token": "test_refresh_token",
                "expires_in": 3600
            }
            """.trimIndent().let { org.json.JSONObject(it) }
        )
        GoogleCalendarDataProvider(context = application).setMainCalendar("test_calendar_id")

        ShadowSettings.setCanDrawOverlays(true)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val serviceId = "${context.packageName}/com.iceiony.CalendarAccessibilityService"

        ShadowSecureSettings.setString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            serviceId)

        val scenario  = ActivityScenario.launchActivityForResult(OnboardingActivity::class.java)

        // Verify that the OnboardingActivity finishes and does not stay in the state
        assert(scenario.result.resultCode in intArrayOf(Activity.RESULT_OK, Activity.RESULT_CANCELED)) {
            "Expected OnboardingActivity finishes as soon as created if all permissions had been granted"
        }

    }

}