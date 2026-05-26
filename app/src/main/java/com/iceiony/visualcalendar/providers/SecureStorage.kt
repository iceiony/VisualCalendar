package com.iceiony.visualcalendar.providers

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.iceiony.visualcalendar.VisualCalendarApp
import kotlinx.coroutines.flow.first

class SecureStorage(
    private val context: Context = VisualCalendarApp.instance.applicationContext
) {
    init {
        AeadConfig.register()
    }

    private val keysetHandle = AndroidKeysetManager.Builder()
        .withSharedPref(context, "master_keyset", "my_pref")
        .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
        .withMasterKeyUri("android-keystore://master_key")
        .build()
        .keysetHandle

    private val aead = keysetHandle.getPrimitive(
        RegistryConfiguration.get(),
        Aead::class.java
    )

    private val Context.dataStore by preferencesDataStore("secure_store")

    suspend fun saveValue(name: String, value: String) {
        val key = stringPreferencesKey(name)
        context.dataStore.edit { prefs ->
            val encryptedBytes = aead.encrypt(value.toByteArray(), null)
            prefs[key] = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        }
    }

    suspend fun getValue(name: String): String? {
        val key = stringPreferencesKey(name)
        val prefs = context.dataStore.data.first()
        return prefs[key]?.let {
            val encryptedBytes = Base64.decode(it, Base64.DEFAULT)
            aead.decrypt(encryptedBytes, null).toString(Charsets.UTF_8)
        }
    }

    suspend fun deleteValue(name: String) {
        val key = stringPreferencesKey(name)
        context.dataStore.edit { prefs ->
            prefs.remove(key)
        }
    }

}