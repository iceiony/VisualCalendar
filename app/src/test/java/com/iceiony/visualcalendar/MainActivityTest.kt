package com.iceiony.visualcalendar

import android.content.Context
import android.os.Build
import android.provider.Settings
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
    fun test_MainActivity_does_not_request_overlay_permissions_when_granted() {
        ShadowSettings.setCanDrawOverlays(true)
        ActivityScenario.launch(MainActivity::class.java)
        intended(not(hasAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)))
    }
}
