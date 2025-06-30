package com.iceiony.visualcalendar

import android.widget.LinearLayout
import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import com.iceiony.visualcalendar.providers.*

class CalendarDayView @JvmOverloads constructor(
    context : Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    dataProvider: DataProvider = iCalDataProvider(),
    timeProvider: TimeProvider = SystemTimeProvider()
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.calendar_day_view, this)

        // Initialize the view with today's date
        val today = timeProvider.now().toLocalDate()
        val dayNameTextView = findViewById<TextView>(R.id.day_name)
        dayNameTextView.text = today.dayOfWeek.name

        // Load today's events from the data provider
        //dataProvider.today()
        //    .subscribe { events ->
        //        // Handle events for today
        //        // For example, update a RecyclerView or ListView with these events
        //    }
    }
}