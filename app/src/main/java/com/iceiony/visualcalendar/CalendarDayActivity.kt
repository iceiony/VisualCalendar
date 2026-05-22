package com.iceiony.visualcalendar

import android.content.Intent
import androidx.activity.ComponentActivity
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.iceiony.visualcalendar.CalendarAccessibilityService.Companion.dataProvider

class CalendarDayActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        Log.d("CalendarDayActivity", "onCreate called")
        super.onCreate(savedInstanceState)

        if(!Permissions.allGranted(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        findViewById<ComposeView>(R.id.calendar_day_view).setContent {
            CalendarDayView(dataProvider)
        }

    }
}
