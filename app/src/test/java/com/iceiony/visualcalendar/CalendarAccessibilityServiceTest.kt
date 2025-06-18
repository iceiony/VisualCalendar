package com.iceiony.visualcalendar

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowWindowManagerImpl


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class CalendarAccessibilityServiceTest {
    @Before
    fun setup() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun test_CalendarAccessibilityService_initialises_an_overlay_view() {
        val service = Robolectric.buildService(CalendarAccessibilityService::class.java).get()
        service.onServiceConnected()

        val shadowWindowManager = Shadows.shadowOf(
            ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        ) as ShadowWindowManagerImpl

        assert(shadowWindowManager.getViews().isNotEmpty()) {
            "Overlay view should be added to the WindowManager"
        }
    }

}

