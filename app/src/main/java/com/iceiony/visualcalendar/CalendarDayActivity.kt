package com.iceiony.visualcalendar

import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class CalendarDayActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        Log.d("CalendarDayActivity", "onCreate called")
        super.onCreate(savedInstanceState)

        if (!Permissions.allGranted(this)) {
            Log.d("CalendarDayActivity", "Permissions not granted, redirecting to PermissionsActivity")

            startActivity(
                Intent(this, OnboardingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )

            finish()

        } else {
            window.decorView.setBackgroundColor(android.graphics.Color.DKGRAY)

            setContent {
                CalendarDayView(VisualCalendarApp.instance.dataProvider)
            }
        }

    }
}
