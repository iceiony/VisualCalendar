package com.iceiony.visualcalendar

import android.app.Application

class VisualCalendarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: VisualCalendarApp
            private set
    }
}
