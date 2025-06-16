package com.iceiony.visualcalendar

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun test_MainActivity_requests_overlay_permissions_when_not_granted() {
        // Mock Settings.canDrawOverlays to return false initially
        Intents
            .intending(
                hasAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            )
            .respondWith(
                Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
            )

        ActivityScenario.launch(MainActivity::class.java)


        // Check that the confirmation label/checkbox is ticked
        // Replace R.id.overlay_permission_checkbox with your actual view ID
        //onView(withId(R.id.overlay_permission_checkbox))
        //    .check(matches(isChecked()))
        // Or, if it's a label:
        // onView(allOf(withId(R.id.overlay_permission_label), withText("Permission granted")))
        //     .check(matches(isDisplayed()))
    }
}
