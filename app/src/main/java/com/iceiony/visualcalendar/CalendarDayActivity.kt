package com.iceiony.visualcalendar

import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class CalendarDayActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        Log.d("CalendarDayActivity", "onCreate called")
        super.onCreate(savedInstanceState)

        setContent {
            CalendarDayView(VisualCalendarApp.instance.dataProvider)
        }

    }
}
