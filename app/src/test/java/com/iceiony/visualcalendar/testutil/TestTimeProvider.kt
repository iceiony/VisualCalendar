package com.iceiony.visualcalendar.testutil

import com.iceiony.visualcalendar.TimeProvider
import io.reactivex.rxjava3.schedulers.TestScheduler
import java.time.LocalDateTime

class TestTimeProvider(
    private var now: LocalDateTime
) : TimeProvider {
    val testScheduler = TestScheduler()

    override fun now(): LocalDateTime {
        return now;
    }

    fun advanceTimeBy(seconds : Long) {
        now = now.plusSeconds(seconds)
        testScheduler.advanceTimeBy(seconds, java.util.concurrent.TimeUnit.SECONDS)
    }
}