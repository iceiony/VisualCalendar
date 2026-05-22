package com.iceiony.visualcalendar.preview

import com.iceiony.visualcalendar.TimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
open class PreviewTimeProvider(
    private var now: LocalDateTime,
    private val scheduler: TestCoroutineScheduler? = null,
) : TimeProvider {

    override fun now(): LocalDateTime {
        return now;
    }

    open fun advanceTimeBy(seconds : Long) {
        now = now.plusSeconds(seconds)
        scheduler?.advanceTimeBy( seconds * 1000 )
    }

    open fun advanceTimeTo(newTime: LocalDateTime) {
        val secondsToAdvance = java.time.Duration.between(now, newTime).seconds
        advanceTimeBy(secondsToAdvance)
    }
}