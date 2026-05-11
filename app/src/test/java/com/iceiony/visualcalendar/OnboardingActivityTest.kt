package com.iceiony.visualcalendar

import android.app.Activity
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import com.iceiony.visualcalendar.testutil.ShadowSecureSettings
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings
import org.robolectric.shadows.ShadowToast


@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowSecureSettings::class], sdk = [Build.VERSION_CODES.S])
class OnboardingActivityTest {
    @Before
    fun setup() {
        Intents.init()
        //ShadowSettings.setCanDrawOverlays(false)
        //ShadowSettings.ShadowSecure.reset();
    }

    @After
    fun tearDown() {
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

        val mainActivity = ActivityScenario.launch(OnboardingActivity::class.java)

        mainActivity.onActivity { activity ->
            activity.overlayPermissionsCallback.onActivityResult(
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
    fun `opens CalendarActivity when permissions are already granted`(){
        ShadowSettings.setCanDrawOverlays(true)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val serviceId = "${context.packageName}/com.iceiony.CalendarAccessibilityService"

        ShadowSecureSettings.setString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            serviceId)

        val mainActivity  = ActivityScenario.launchActivityForResult(OnboardingActivity::class.java)

        // Verify that the OnboardingActivity finishes and does not stay in the state
        assert(mainActivity.result.resultCode in intArrayOf(Activity.RESULT_OK, Activity.RESULT_CANCELED)) {
            "Expected OnboardingActivity finishes as soon as created if all permissions had been granted"
        }
    }

    @Test
    fun `finishes the activity when all permissions granted`() {
        ShadowSettings.setCanDrawOverlays(false)
        ShadowSettings.reset()

        val mainActivity = ActivityScenario.launch(OnboardingActivity::class.java)

        // Simulate granting overlay permission
        ShadowSettings.setCanDrawOverlays(true)
        mainActivity.onActivity { activity ->
            activity.overlayPermissionsCallback.onActivityResult(
                ActivityResult(Activity.RESULT_OK, null)
            )
        }

        //simulate granting accessibility permission
        val context = ApplicationProvider.getApplicationContext<Context>()
        val serviceId = "${context.packageName}/com.iceiony.CalendarAccessibilityService"
        ShadowSecureSettings.setString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            serviceId
        )

        mainActivity.onActivity {
            activity ->
            activity.accessibilityPermissionsCallback.onActivityResult(
                ActivityResult(Activity.RESULT_OK, null)
            )
        }


        // Verify the activity is finished
        mainActivity.onActivity { activity ->
            assert(activity.isFinishing) {
                "Expected OnboardingActivity to finish when all permissions are granted"
            }
        }
    }
}