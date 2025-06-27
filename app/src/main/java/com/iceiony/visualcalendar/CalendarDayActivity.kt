package com.iceiony.visualcalendar

import android.content.Intent
import android.provider.Settings
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class CalendarDayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        if(Permissions.allGranted(this)) {
            setContentView(R.layout.activity_calendar_day)
        } else {
            startActivity(Intent(this, CalendarDayActivity::class.java))
        }
    }
}
