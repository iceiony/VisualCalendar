package com.iceiony.visualcalendar

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView

class CalendarDayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        Log.d("CalendarDayActivity", "onCreate called")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_calendar_day)

        findViewById<ComposeView>(R.id.calendar_day_view).setContent {
            CalendarDayView()
        }

        if(!Permissions.allGranted(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

    }
}
