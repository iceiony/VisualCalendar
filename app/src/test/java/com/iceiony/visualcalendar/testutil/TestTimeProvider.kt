package com.iceiony.visualcalendar.testutil

import android.content.Context
import androidx.work.WorkManager
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import com.iceiony.visualcalendar.providers.TimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import java.time.Duration
import java.time.LocalDateTime

// An overly complicated workaround to the fact that the WorkManager's TestDriver does not support simulating time advancing
@OptIn(ExperimentalCoroutinesApi::class)
class TestTimeProvider(
    var now: LocalDateTime,
    var scheduler: TestCoroutineScheduler? = null,
    context: Context,
) : TimeProvider {

    private var totalTimePassedMillis: Long = 0
    private var workManager: WorkManager = WorkManager.getInstance(context)
    private var testDriver: TestDriver = WorkManagerTestInitHelper.getTestDriver(context)!!

    override fun now(): LocalDateTime {
        return now;
    }

    fun advanceTimeBy(seconds : Long) {
        now = now.plusSeconds(seconds)
        scheduler?.advanceTimeBy( seconds * 1000 )

        totalTimePassedMillis += seconds * 1000
        val workInfos = workManager.getWorkInfosByTag("com.iceiony.visualcalendar").get()
        for (workInfo in workInfos) {
            if(workInfo.state.name != "ENQUEUED") continue
            if(workInfo.initialDelayMillis < totalTimePassedMillis) {
                testDriver.setInitialDelayMet(workInfo.id)
            }
        }

        now = now.plusSeconds(0)
        scheduler?.advanceTimeBy( 0)
    }


    fun advanceTimeTo(newTime: LocalDateTime) {
        val secondsToAdvance = Duration.between(now, newTime).seconds
        advanceTimeBy(secondsToAdvance)
    }

}