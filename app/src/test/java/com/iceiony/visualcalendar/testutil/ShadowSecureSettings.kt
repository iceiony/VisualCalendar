package com.iceiony.visualcalendar.testutil

import android.content.ContentResolver
import android.provider.Settings
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowSettings

@Implements(ShadowSettings.ShadowSecure::class)
class ShadowSecureSettings {
    companion object {
        @JvmStatic
        fun setString(cr: ContentResolver, name: String, value: String) {
            Settings.Secure.putString(cr, name, value)
        }

    }
}