package com.iceiony.visualcalendar.providers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Duration
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewTimeProvider(
    private var now: LocalDateTime,
) : TimeProvider {

    override fun now(): LocalDateTime {
        return now;
    }

    fun advanceTimeBy(seconds : Long) {
        now = now.plusSeconds(seconds)
        //scheduler?.advanceTimeBy( seconds * 1000 )
    }

    fun advanceTimeTo(newTime: LocalDateTime) {
        val secondsToAdvance = Duration.between(now, newTime).seconds
        advanceTimeBy(secondsToAdvance)
    }
}