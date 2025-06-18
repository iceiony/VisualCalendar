package com.iceiony.visualcalendar

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowSettings
import org.robolectric.shadows.ShadowToast

@Implements(ShadowSettings.ShadowSecure::class)
class ShadowSecureSettings {
    companion object {
        @JvmStatic
        fun setString(cr: ContentResolver, name: String, value: String) {
            Settings.Secure.putString(cr, name, value)
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowSecureSettings::class], sdk = [Build.VERSION_CODES.S])
class MainActivityTest {
    @Before
    fun setup() {
        Intents.init()
        //ShadowSettings.setCanDrawOverlays(false)
        //ShadowSettings.ShadowSecure.reset();
        //ShadowSettings.reset()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun test_MainActivity_requests_overlay_permissions_when_not_granted() {
        ShadowSettings.setCanDrawOverlays(false)
        ActivityScenario.launch(MainActivity::class.java)

        val latestToastText = ShadowToast.getTextOfLatestToast()
        assert(latestToastText == "Please enable overlay permission for Visual Calendar")

        intended(hasAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        ShadowSettings.setCanDrawOverlays(true)
    }

    @Test
    fun test_MainActivity_creates_toast_when_overlay_permission_not_granted() {
        ShadowSettings.setCanDrawOverlays(false)

        ActivityScenario
            .launch(MainActivity::class.java)
            .onActivity { activity ->
                // Simulate the callback directly (Robolectric can't "launch" external permissions)
                activity.overlayPermissionCallback.onActivityResult(
                    ActivityResult(Activity.RESULT_CANCELED, null)
                )

                //use robolectric to verify the toast message
                val latestToastText = ShadowToast.getTextOfLatestToast()
                assert(latestToastText == "Permission not granted to show overlay")
            }
    }

    @Test
    fun test_MainActivity_requests_accessibility_when_overlay_already_granted_but_not_accessibility() {
        ShadowSettings.setCanDrawOverlays(true)
        ActivityScenario.launch(MainActivity::class.java)
        intended(hasAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION), Intents.times(0))

        intended(hasAction(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    @Test
    fun test_MainActivity_does_not_open_accessibility_settings_when_already_granted() {
        ShadowSettings.setCanDrawOverlays(true)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val serviceId = "${context.packageName}/com.iceiony.CalendarAccessibilityService"

        ShadowSecureSettings.setString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            serviceId)

        ActivityScenario.launch(MainActivity::class.java)

        intended(hasAction(Settings.ACTION_ACCESSIBILITY_SETTINGS), Intents.times(0))
    }
}