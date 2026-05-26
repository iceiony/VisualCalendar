package com.iceiony.visualcalendar

import java.time.LocalDateTime

interface TimeProvider {
    fun now(): LocalDateTime
}

class SystemTimeProvider : TimeProvider {
    override fun now(): LocalDateTime = LocalDateTime.now()
}