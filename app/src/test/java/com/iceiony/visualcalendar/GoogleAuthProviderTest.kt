package com.iceiony.visualcalendar

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import app.cash.turbine.test
import com.iceiony.visualcalendar.providers.google.GoogleAuthProvider
import com.iceiony.visualcalendar.testutil.TestInterceptor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
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

        val deviceCodeInfo = authProvider.requestDeviceCode().first()

        assert(deviceCodeInfo.deviceCode.isNotEmpty())
        assert(deviceCodeInfo.userCode.isNotEmpty())
        assert(deviceCodeInfo.verificationUrl.isNotEmpty())
        assert(deviceCodeInfo.intervalSeconds > 0)
    }

    @Test
    fun `device code updates after expiration duration passes with no authorisation`() = runTest {
        val client = OkHttpClient.Builder()
            .addInterceptor(
                TestInterceptor(
                    "/token",
                    //response mocked from real API response
                    body = """
                        {
                            "error" : "authorization_pending",
                            "error_description" : "Precondition Required"
                        }
                    """
                )
            ).build()

        val authProvider = GoogleAuthProvider(context, client = client)

        authProvider.requestDeviceCode().test {
            val firstDeviceInfo = awaitItem()
            println("First device code: ${firstDeviceInfo.deviceCode}")

            advanceTimeBy(firstDeviceInfo.expiresIn * 1000L - 1000L) // Advance time to just before the refresh

            expectNoEvents()

            advanceTimeBy(2000L)

            val secondDeviceInfo = awaitItem()
            println("Second device code: ${secondDeviceInfo.deviceCode}")

            assert(firstDeviceInfo.deviceCode != secondDeviceInfo.deviceCode)
        }
    }

    @Test
    fun `authorisation token saved when user approves device`() = runTest{
        val client = OkHttpClient.Builder()
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
            ).build()

        val authProvider = GoogleAuthProvider(context, client = client)

        authProvider.requestDeviceCode().test {
            val firstDeviceInfo = awaitItem()

            advanceTimeBy(firstDeviceInfo.intervalSeconds * 1000L + 1000L) // Advance time to just after the first poll

            awaitComplete() // should complete after receiving the token
        }

        assert(
            authProvider.secureStorage.getValue("access_token") == "fake access token"
        )

        assert(
            authProvider.secureStorage.getValue("refresh_token") == "fake refresh token"
        )

        assert( authProvider.prefs.contains("token_expiry") )

        //check the token expires in the future
        val expiryTime = authProvider.prefs.getLong("token_expiry", 0L)
        assert( expiryTime > System.currentTimeMillis() / 1000L )
    }

}