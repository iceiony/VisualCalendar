package com.iceiony.visualcalendar

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.iceiony.visualcalendar.providers.AuthProvidier
import com.iceiony.visualcalendar.testutil.ShadowSecureSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings
import kotlin.String
import kotlin.intArrayOf

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowSecureSettings::class], sdk = [Build.VERSION_CODES.S])
class PermissionsChecklistViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @After
    fun tearDown() {}

    private fun unauthorisedAuthProvider() = object : AuthProvidier {
        override fun requestDeviceCode(): Flow<AuthProvidier.DeviceCodeInfo> {
            return flow {
                emit(
                    AuthProvidier.DeviceCodeInfo(
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
            PermissionsChecklistView( authProvider =  unauthorisedAuthProvider())
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodes(isToggleable())
            .assertCountEquals(3)

        composeTestRule
            .onAllNodes(isToggleable())
            .assertAll(isOff())
    }

    @Test
    fun `has overlay and accessibility permissions ticked and QR for authorisation`() {
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
            PermissionsChecklistView( authProvider =  unauthorisedAuthProvider() )
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


}