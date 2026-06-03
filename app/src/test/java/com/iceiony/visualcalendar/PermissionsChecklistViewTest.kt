package com.iceiony.visualcalendar

import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.iceiony.visualcalendar.providers.AuthProvider
import com.iceiony.visualcalendar.providers.google.GoogleAuthProvider
import com.iceiony.visualcalendar.providers.google.GoogleCalendarDataProvider
import com.iceiony.visualcalendar.testutil.ShadowSecureSettings
import com.iceiony.visualcalendar.viewmodels.PermissionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowSecureSettings::class], sdk = [Build.VERSION_CODES.S])
@OptIn(ExperimentalCoroutinesApi::class)
class PermissionsChecklistViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    private lateinit var context: Context

    @Before
    fun setup() {
        Intents.init()

        Dispatchers.setMain(StandardTestDispatcher())
        context = ApplicationProvider.getApplicationContext<Application>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @After
    fun tearDown() {
        WorkManager.getInstance(context).cancelAllWork()
        WorkManagerTestInitHelper.closeWorkDatabase()
        Dispatchers.resetMain()

        Intents.release()
    }

    private fun unauthorisedAuthProvider() = object : AuthProvider {
        override fun requestDeviceCode(): Flow<AuthProvider.DeviceCodeInfo> {
            return flow {
                emit(
                    AuthProvider.DeviceCodeInfo(
                        deviceCode = "test_device_code",
                        userCode = "test_user_code",
                        verificationUrl = "https://example.com/verify",
                        intervalSeconds = 5,
                        expiresIn = 30
                    )
                )
            }
        }

        override suspend fun getValidAccessToken(): String? = null

        override fun isAuthorised(): Boolean = false
    }

    @Test
    fun `has no CheckBox ticked when no permissions granted`()  {
        composeTestRule.setContent {
            PermissionsChecklistView(
                viewModel = PermissionsViewModel(
                    context,
                    authProvider = unauthorisedAuthProvider()
                )
            )
        }

        //advanceUntilIdle()
        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodes(isToggleable())
            .assertCountEquals(3)

        composeTestRule
            .onAllNodes(isToggleable())
            .assertAll(isOff())
    }


    @Test
    fun `requests overlay permissions on row tap`() {
        composeTestRule.setContent {
            PermissionsChecklistView(
                viewModel = PermissionsViewModel(
                    context,
                    authProvider = unauthorisedAuthProvider()
                )
            )
        }

        //locate the checkbox row by finding the text and then getting its parent
        composeTestRule
            .onNodeWithText("Overlay Permission")
            .performClick()

        composeTestRule.waitForIdle()

        val latestToastText = ShadowToast.getTextOfLatestToast()
        assert(latestToastText == "Please enable overlay permission for Visual Calendar") {
            "Expected toast message to be shown when overlay permission is not granted, but got: $latestToastText"
        }

        intended(hasAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        ShadowSettings.setCanDrawOverlays(true)
    }


    @Test
    fun `requests accessibility service enabling on row tap`() {
        composeTestRule.setContent {
            PermissionsChecklistView(
                viewModel = PermissionsViewModel(
                    context,
                    authProvider = unauthorisedAuthProvider()
                )
            )
        }

        //locate the checkbox row by finding the text and then getting its parent
        composeTestRule
            .onNodeWithText("Accessibility Service Permission")
            .performClick()

        composeTestRule.waitForIdle()

        val latestToastText = ShadowToast.getTextOfLatestToast()
        assert(latestToastText == "Please enable Visual Calendar in Accessibility Services") {
            "Expected toast message to be shown when overlay permission is not granted, but got: $latestToastText"
        }

        intended(hasAction(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    @Test
    fun `can have overlay and accessibility permissions ticked but QR displayed for calendar authorisation`() {
        //pretend overlay permissions granted
        ShadowSettings.setCanDrawOverlays(true)

        //pretend accessibility service enabled
        val serviceId = "${context.packageName}/com.iceiony.CalendarAccessibilityService"
        ShadowSecureSettings.setString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            serviceId
        )

        //inflate view
        composeTestRule.setContent {
            PermissionsChecklistView(
                viewModel = PermissionsViewModel(
                    context,
                    authProvider = unauthorisedAuthProvider()
                )
            )
        }

        composeTestRule.waitForIdle()

        //check correct permissions are ticked
        val checkedCount = composeTestRule
            .onAllNodes(isToggleable() and isOn())
            .fetchSemanticsNodes()
            .size

        assert(checkedCount == 2) {
            "Expected 2 permissions to be checked, but found $checkedCount."
        }

        composeTestRule
            .onNodeWithText("Scan QR or use code on separate device.")
            .assertExists()
    }

    @Test
    fun `shows a list of calendars when already authenticated`()  = runTest {
        //pretend calendar access granted with valid token
        val authProvider = GoogleAuthProvider(context)
        authProvider.setAuthState(
            """
                {
                  "access_token" : "${BuildConfig.TEST_ACCESS_TOKEN}" ,
                  "expires_in" : ${ -60 } ,
                  "refresh_token" : "${BuildConfig.TEST_REFRESH_TOKEN}" ,
                  "scope" : "https://www.googleapis.com/auth/calendar.readonly",
                  "token_type" : "Bearer"
                }
            """.trimIndent().let { JSONObject(it) }
        )

        val dataProvider = GoogleCalendarDataProvider(
            context, authProvider = authProvider,
        )

        val viewModel = PermissionsViewModel(
            context,
            authProvider = authProvider,
            dataProvider = dataProvider
        )

        composeTestRule.waitForIdle()

        //inflate view
        composeTestRule.setContent {
            PermissionsChecklistView( viewModel = viewModel)
        }

        //waitForCoroutineExecution()

        composeTestRule.onNodeWithText("Select the calendar you want to display.").assertExists()

    }

    private suspend fun TestScope.waitForCoroutineExecution(times : Int = 4) {
        advanceUntilIdle()
        composeTestRule.waitForIdle()
        delay(100L)

        for (i in 1..times) {
            advanceTimeBy(1L)
            composeTestRule.waitForIdle()
            delay(100L)
        }
    }

}