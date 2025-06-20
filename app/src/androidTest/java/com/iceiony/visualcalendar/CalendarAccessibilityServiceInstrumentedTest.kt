package com.iceiony.visualcalendar;

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeoutException


/**
 * Instrumented test to execute accessibility service on the actual device so that events and values are not mocked.
 * This would allow for testing on individual devices and new android versions.
 *
 */
@RunWith(AndroidJUnit4::class)
class CalendarAccessibilityServiceInstrumentedTest {

    @Rule
    @JvmField
     val mServiceRule: ServiceTestRule = ServiceTestRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext;
        val serviceIntent = Intent(context, CalendarAccessibilityService::class.java)

        InstrumentationRegistry
            .getInstrumentation().uiAutomation
            .executeShellCommand("settings put secure enabled_accessibility_services ${serviceIntent.component?.flattenToString()}")
            .close()

        InstrumentationRegistry
            .getInstrumentation().uiAutomation
            .executeShellCommand("settings put secure accessibility_enabled 1")
            .close()

        val binder = mServiceRule.bindService(serviceIntent)

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        // Force the system to re-evaluate enabled accessibility services
        val pm = context.packageManager
        val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        val resolveInfo = pm.resolveActivity(settingsIntent, 0)

        val settingsApp = resolveInfo?.activityInfo?.let {
            "${it.packageName}/${it.name}"
        }

        if (settingsApp != null) {
            InstrumentationRegistry
                .getInstrumentation().uiAutomation
                .executeShellCommand( "am start -n $settingsApp" )
                .close()

            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Thread.sleep(5000) // Wait for the settings app to open

            InstrumentationRegistry
                .getInstrumentation().uiAutomation
                .executeShellCommand("cmd accessibility reload").close()

            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Thread.sleep(5000) // Wait for the settings app to open

            InstrumentationRegistry
                .getInstrumentation().uiAutomation
                .performGlobalAction(
                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
                )

            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Thread.sleep(10000)
        }
    }

    @Test
    @Throws(TimeoutException::class)
    fun test_CalendarAccessibilityService_is_running() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        assertTrue("Service should be created", prefs.getBoolean("service_created", false))
        assertTrue("Service should be connected", prefs.getBoolean("service_connected", false))

        //val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
        //assertNotNull("Accessibility Service should be available", accessibilityManager)

        ////check if service view is visible using reflection ( since it is private)
        //val service = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as CalendarAccessibilityService

        //val overlayField = service.javaClass.getDeclaredField("overlayView")
        //overlayField.isAccessible = true

        //val overlayView = overlayField.get(service) as? View

        //assertTrue("Overlay view should be visible", overlayView?.isShown() ?: false)

    }

    @After
    fun tearDown() {
        val serviceIntent = Intent(context, CalendarAccessibilityService::class.java)
        context.stopService(serviceIntent)
    }
}
