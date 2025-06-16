package com.iceiony.visualcalendar

import android.app.Activity
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowToast

@Implements(Settings::class)
class ShadowSettings {
    companion object {
        private var canDrawOverlays = false

        @Implementation
        @JvmStatic
        fun canDrawOverlays(context: Context): Boolean {
            return canDrawOverlays
        }

        @JvmStatic
        fun setCanDrawOverlays(value: Boolean) {
            canDrawOverlays = value
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowSettings::class], sdk = [Build.VERSION_CODES.S])
class MainActivityTest {
    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun test_MainActivity_requests_overlay_permissions_when_not_granted() {
        ShadowSettings.setCanDrawOverlays(false)
        ActivityScenario.launch(MainActivity::class.java)
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
    fun test_MainActivity_requests_accessibility_when_overlay_already_granted() {
        ShadowSettings.setCanDrawOverlays(true)
        ActivityScenario.launch(MainActivity::class.java)
        intended(not(hasAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)))

        intended(hasAction(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
