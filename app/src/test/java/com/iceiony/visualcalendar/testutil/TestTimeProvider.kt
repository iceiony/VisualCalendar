package com.iceiony.visualcalendar.testutil

import android.content.Context
import androidx.work.WorkManager
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import com.iceiony.visualcalendar.preview.PreviewTimeProvider
import io.reactivex.rxjava3.schedulers.TestScheduler
import java.time.LocalDateTime

// An overly complicated workaround to the fact that the WorkManager's TestDriver does not support simulating time advancing
class TestTimeProvider(
    now: LocalDateTime,
    scheduler: TestScheduler = TestScheduler(),
    context: Context
) : PreviewTimeProvider(now, scheduler) {

    private var totalTimePassedMillis: Long = 0
    private var workManager: WorkManager = WorkManager.getInstance(context)
    private var testDriver: TestDriver = WorkManagerTestInitHelper.getTestDriver(context)!!

    override fun advanceTimeBy(seconds : Long) {
        totalTimePassedMillis += seconds * 1000
        val workInfos = workManager.getWorkInfosByTag("com.iceiony.visualcalendar").get()
        for (workInfo in workInfos) {
            if(workInfo.state.name != "ENQUEUED") continue
            if(workInfo.initialDelayMillis < totalTimePassedMillis) {
                testDriver.setInitialDelayMet(workInfo.id)
            }
        }

        super.advanceTimeBy(seconds)
    }

}