package com.iceiony.visualcalendar

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.iceiony.visualcalendar.local_storage.SecureStorage
import kotlinx.coroutines.flow.first
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
class SecureStorageTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    @After
    fun tearDown() { }

    @Test
    fun `can store and retrieve strings`() = runTest {
        val storage = SecureStorage(context)

        val key = "test_key"
        val value = "Hello, Secure Storage!"

        storage.saveValue(key, value)
        val retrievedValue = storage.getValue(key).first()

        println("Decrypted value: $retrievedValue")
        assert(retrievedValue == value)
    }

    @Test
    fun `can not retrieve string before storage`() = runTest {
        val storage = SecureStorage(context)

        val key = "non_existent_key"

        val retrievedValue = storage.getValue(key).first()

        println("Decrypted value for non-existent key: $retrievedValue")
        assert(retrievedValue == null)
    }
}