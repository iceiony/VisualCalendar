package com.iceiony.visualcalendar

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
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

        assert(shadowWindowManager.views.isNotEmpty()) {
            "Overlay view should be added to the WindowManager"
        }

        service.onDestroy()

        assert(shadowWindowManager.views.isEmpty()) {
            "Overlay view should be removed from the WindowManager on service destruction"
        }

    }

    @Test
    fun test_CalendarAccessibilityService_deflates_view_when_other_apps_are_opened() {
        val service = Robolectric.buildService(CalendarAccessibilityService::class.java).get()
        service.onServiceConnected()

        // Simulate opening another app
        service.onAccessibilityEvent(
            AccessibilityEvent().apply {
                eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                packageName = "com.example.otherapp"
            }
        )

        val shadowWindowManager = Shadows.shadowOf(
            ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        ) as ShadowWindowManagerImpl

        assert(shadowWindowManager.views.isNotEmpty()) {
            "Overlay view should still exist when other apps are opened"
        }

        val view = shadowWindowManager.views.firstOrNull()

        assert(view?.visibility == android.view.View.GONE) {
            "Overlay view should be hidden when other apps are opened"
        }

        //simulate going back to the lock screen on a fire os kids tablet
        service.onAccessibilityEvent(
            AccessibilityEvent().apply {
                eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                packageName = "com.amazon.tahoe"
            }
        )
        //simulate the visual calendar widget being opened
        service.onAccessibilityEvent(
            AccessibilityEvent().apply {
                eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                packageName = "com.iceiony.visualcalendar"
                className = "android.widget.FrameLayout"
            }
        )


        assert(view?.visibility == android.view.View.VISIBLE) {
            "Overlay view should be visible when home screen is opened"
        }

        //simulate going to another app
        service.onAccessibilityEvent(
            AccessibilityEvent().apply {
                eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                packageName = "com.example.anotherapp"
            }
        )

        assert(view?.visibility == android.view.View.GONE) {
            "Overlay view should still be visible on the lock screen"
        }
    }

}

