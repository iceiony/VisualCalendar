package com.iceiony.visualcalendar.preview

import android.content.Context
import com.iceiony.visualcalendar.TimeProvider
import io.reactivex.rxjava3.schedulers.TestScheduler
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

open class PreviewTimeProvider(
    private var now: LocalDateTime,
    protected val scheduler: TestScheduler = TestScheduler()
) : TimeProvider {


    override fun now(): LocalDateTime {
        return now;
    }

    open fun advanceTimeBy(seconds : Long) {
        now = now.plusSeconds(seconds)
        scheduler.advanceTimeBy(seconds, TimeUnit.SECONDS)

    }
}