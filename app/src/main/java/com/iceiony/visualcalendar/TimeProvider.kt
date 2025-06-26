package com.iceiony.visualcalendar

import io.reactivex.rxjava3.core.Observable
import java.time.LocalDateTime

interface TimeProvider {
    fun now(): LocalDateTime
}

class SystemTimeProvider : TimeProvider {
    override fun now(): LocalDateTime = LocalDateTime.now()
}