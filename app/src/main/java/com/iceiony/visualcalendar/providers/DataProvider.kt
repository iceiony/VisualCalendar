package com.iceiony.visualcalendar.providers

import biweekly.component.VEvent
import biweekly.property.DateEnd
import biweekly.property.DateStart
import biweekly.util.ICalDate
import io.reactivex.rxjava3.core.Observable
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

interface DataProvider {
    fun today(): Observable<List<VEvent>>
    fun refresh()
}


fun DateStart.toTime(): String {
    return SimpleDateFormat("HH:mm").format(value as Date)
}

fun DateEnd.toTime(): String {
    return SimpleDateFormat("HH:mm").format(value as Date)
}
