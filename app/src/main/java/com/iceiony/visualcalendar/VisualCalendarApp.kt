package com.iceiony.visualcalendar

import android.app.Application
import com.iceiony.visualcalendar.providers.DataProvider
import com.iceiony.visualcalendar.providers.google.GoogleCalendarDataProvider
import kotlin.lazy

class VisualCalendarApp : Application() {
    val dataProvider: DataProvider by lazy {
        GoogleCalendarDataProvider(applicationContext)
    }

    companion object {
        lateinit var instance: VisualCalendarApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onTerminate() {
        super.onTerminate()
        dataProvider.destroy()
    }
}
