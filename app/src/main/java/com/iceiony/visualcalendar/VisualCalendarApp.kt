package com.iceiony.visualcalendar

import android.app.Application
import android.util.Log
import com.iceiony.visualcalendar.providers.DataProvider
import com.iceiony.visualcalendar.providers.google.GoogleAuthProvider
import com.iceiony.visualcalendar.providers.google.GoogleCalendarDataProvider
import kotlin.lazy

class VisualCalendarApp : Application() {

    private val authProviderLazy = lazy {
        GoogleAuthProvider(applicationContext)
    }
    private val dataProviderLazy = lazy {
        GoogleCalendarDataProvider(applicationContext, authProvider = authProvider)
    }
    val dataProvider: DataProvider by dataProviderLazy
    val authProvider: GoogleAuthProvider by authProviderLazy

    companion object {
        lateinit var instance: VisualCalendarApp
            private set
    }

    override fun onCreate() {
        Log.i("VisualCalendarApp", "Application onCreate called")
        super.onCreate()
        instance = this
    }

    override fun onTerminate() {
        super.onTerminate()
        if (dataProviderLazy.isInitialized()) {
            dataProvider.destroy()
        }
    }
}
