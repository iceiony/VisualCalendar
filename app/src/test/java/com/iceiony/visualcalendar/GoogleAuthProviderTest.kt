package com.iceiony.visualcalendar

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import com.iceiony.visualcalendar.providers.google.GoogleAuthProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.intArrayOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
class GoogleAuthProviderTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun `launches an Onboarding activity when no refresh token exist`() = runTest {
        val authProvider = GoogleAuthProvider(context)

        authProvider.clearAuthState() // Ensure no tokens are stored

        val token = authProvider.getValidAccessToken()

        assert(token == null) // Should return null since no token is available

        intended(hasComponent(OnboardingActivity::class.java.name))
    }

    @Test
    fun `can request device code`() = runTest {
        val authProvider = GoogleAuthProvider(context)

        val deviceCodeInfo = authProvider.requestDeviceCode()

        assert(deviceCodeInfo.deviceCode.isNotEmpty())
        assert(deviceCodeInfo.userCode.isNotEmpty())
        assert(deviceCodeInfo.verificationUrl.isNotEmpty())
        assert(deviceCodeInfo.intervalSeconds > 0)
    }
}