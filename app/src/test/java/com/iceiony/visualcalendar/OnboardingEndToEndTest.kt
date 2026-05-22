package com.iceiony.visualcalendar

import android.app.Activity
import android.app.Application
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.iceiony.visualcalendar.providers.google.GoogleAuthProvider
import com.iceiony.visualcalendar.providers.google.GoogleCalendarDataProvider
import com.iceiony.visualcalendar.testutil.ShadowSecureSettings
import com.iceiony.visualcalendar.testutil.TestInterceptor
import com.iceiony.visualcalendar.viewmodels.PermissionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings


@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowSecureSettings::class], sdk = [Build.VERSION_CODES.S])
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingEndToEndTest {



    @get:Rule
    val composeTestRule = createEmptyComposeRule()
    private lateinit var application: Application
    private lateinit var controller: ActivityController<OnboardingActivity>
    private lateinit var activity: OnboardingActivity

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())

        Intents.init()

        application = ApplicationProvider.getApplicationContext<Application>()

        WorkManagerTestInitHelper.initializeTestWorkManager(application)

        ShadowSettings.reset()
        ShadowSettings.setCanDrawOverlays(false)

        controller = Robolectric.buildActivity(OnboardingActivity::class.java)
        activity = controller.get()

        val client = OkHttpClient.Builder()
            .addInterceptor(
                TestInterceptor(
                    "/device/code",
                    //response mocked from real API response
                    body = """
                        {
                            "device_code": "fake_device_code",
                            "user_code": "fake_user_code",
                            "verification_url": "https://www.google.com/device",
                            "interval": 5,
                            "expires_in": 3600
                        }
                    """
                )
            )
            .addInterceptor(
                TestInterceptor(
                    "/token",
                    //response mocked from real API response
                    body = """
                        {
                            "access_token": "fake access token",
                            "expires_in": 3600,
                            "refresh_token": "fake refresh token",
                            "scope": "https://www.googleapis.com/auth/calendar.readonly",
                            "token_type": "Bearer"
                        }
                    """
                )
            )
            .addInterceptor(
                TestInterceptor(
                    path = "calendar/v3/users/me/calendarList",
                    body = """
                        {
                            "items": [
                                {
                                    "id": "primary",
                                    "summary": "Primary Calendar"
                                },
                                {
                                    "id": "work",
                                    "summary": "Work Calendar"
                                }
                            ]
                        }
                    """
                )
            )
            .build()

        val authProvider = GoogleAuthProvider(context = application, client = client)
        val dataProvider = GoogleCalendarDataProvider(
            application,
            authProvider = authProvider,
            client = client
        )

        val injectedViewModel = PermissionsViewModel(
            application,
            authProvider = authProvider,
            dataProvider = dataProvider
        )

        ViewModelProvider(activity.viewModelStore, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return injectedViewModel as T
            }
        })[PermissionsViewModel::class.java]
    }

    @After
    fun tearDown() {
        WorkManager.getInstance(application).cancelAllWork()
        WorkManagerTestInitHelper.closeWorkDatabase()
        Intents.release()
        Dispatchers.resetMain()
    }

    private fun TestScope.waitForCoroutineExecution(times : Int = 4) {
        advanceUntilIdle()
        composeTestRule.waitForIdle()
        for (i in 1..times) {
            advanceTimeBy(1L)
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun `finishes the activity when all permissions are granted`()  = runTest {
        controller.setup()
        composeTestRule.waitForIdle()

        var checkedCount = composeTestRule
            .onAllNodes(isToggleable() and isOn())
            .fetchSemanticsNodes()
            .size

        assert(checkedCount == 0) {
            "Expected 0 permissions to be enabled initially, but found $checkedCount."
        }

        composeTestRule.onNodeWithText("Select the calendar you want to display.").assertDoesNotExist()

        // advance time so mock authentication responses are processed
        waitForCoroutineExecution()

        composeTestRule.onNodeWithText("Select the calendar you want to display.").assertExists()

        checkedCount = composeTestRule
            .onAllNodes(isToggleable() and isOn())
            .fetchSemanticsNodes()
            .size

        assert(checkedCount == 0) {
            "Still expecting 0 permissions enabled as main calendar not selected, but found $checkedCount."
        }

        composeTestRule.onNodeWithText("Primary Calendar").performClick()
        composeTestRule.waitForIdle()

        checkedCount = composeTestRule
            .onAllNodes(isToggleable() and isOn())
            .fetchSemanticsNodes()
            .size

        assert(checkedCount == 1) {
            "Expect 1 permission enabled for calendar selection, but found $checkedCount."
        }

        // Simulate granting overlay permission
        ShadowSettings.setCanDrawOverlays(true)
        activity.viewModel.overlayPermissionsCallback.onActivityResult(
            ActivityResult(Activity.RESULT_OK, Intent())
        )

        composeTestRule.waitForIdle()
        checkedCount = composeTestRule
            .onAllNodes(isToggleable() and isOn())
            .fetchSemanticsNodes()
            .size

        assert(checkedCount == 2) {
            "Expected 2 permission to be checked after overlay granted, but found $checkedCount."
        }


        //simulate granting accessibility permission
        val serviceId = "${application.packageName}/com.iceiony.CalendarAccessibilityService"
        ShadowSecureSettings.setString(
            application.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            serviceId
        )

        activity.viewModel.accessibilityPermissionsCallback.onActivityResult(
            ActivityResult(Activity.RESULT_OK, null)
        )

        waitForCoroutineExecution()

        checkedCount = composeTestRule
            .onAllNodes(isToggleable() and isOn())
            .fetchSemanticsNodes()
            .size

        assert(checkedCount == 3) {
            "Expected 3 permissions to be checked after accessibility granted, but found $checkedCount"
        }

        // Verify the activity is finished
        assert(activity.isFinishing) {
            "Expected OnboardingActivity to finish when all permissions are granted"
        }
    }
}