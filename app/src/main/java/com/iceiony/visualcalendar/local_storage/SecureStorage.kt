package com.iceiony.visualcalendar.local_storage

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import com.google.crypto.tink.Aead
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.aead.AeadConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SecureStorage(private val context: Context) {
    init {
        AeadConfig.register()
    }

    val keysetHandle = AndroidKeysetManager.Builder()
        .withSharedPref(context, "master_keyset", "my_pref")
        .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
        .withMasterKeyUri("android-keystore://master_key")
        .build()
        .keysetHandle

    val aead = keysetHandle.getPrimitive(
        RegistryConfiguration.get(),
        Aead::class.java
    )


    private val Context.dataStore by preferencesDataStore("secure_store")

    private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")

    suspend fun saveValue(name: String, value: String) {
        val key = stringPreferencesKey(name)
        context.dataStore.edit { prefs ->
            val encryptedBytes = aead.encrypt(value.toByteArray(), null)
            prefs[key] = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        }
    }

    fun getValue(name: String): Flow<String?> {
        val key = stringPreferencesKey(name)
        return context.dataStore.data
            .map { prefs ->
                prefs[key]?.let {
                    val encryptedBytes = Base64.decode(it, Base64.DEFAULT)
                    aead.decrypt(encryptedBytes, null).toString(Charsets.UTF_8)
                }
            }
    }

}
