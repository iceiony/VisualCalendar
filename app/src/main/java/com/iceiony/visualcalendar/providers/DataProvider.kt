package com.iceiony.visualcalendar.providers

import android.content.Context
import java.util.concurrent.TimeUnit
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import biweekly.component.VEvent
import biweekly.property.DateEnd
import biweekly.property.DateStart
import biweekly.util.ICalDate
import com.iceiony.visualcalendar.SystemTimeProvider
import com.iceiony.visualcalendar.TimeProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.ReplaySubject
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

interface DataProvider {
    fun today(context: Context): Observable<List<VEvent>>
    fun refresh(now: LocalDateTime)
    fun dispose()
}

abstract class ScheduledDataProvider(
    protected val timeProvider: TimeProvider,
    protected val scheduler : Scheduler,
) : DataProvider {

    protected val client = OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(30))
        .build()
    protected val subject = ReplaySubject.create<List<VEvent>>(1)

    companion object {
        @Volatile
        private lateinit var _instance: ScheduledDataProvider

        private var _isActive = false
    }

    init {
        _instance = this
        _isActive  = true

        val now = timeProvider.now()
        if (now.hour < 18) {
            refresh(now)
        } else {
            refresh(now.plusDays(1))
        }
    }

    override fun refresh(now: LocalDateTime) {
        Observable
            .fromCallable{ getDaysEvents(now) }
            .subscribeOn(scheduler)
            .observeOn(scheduler)
            .subscribe(
                { events -> subject.onNext(events) },
                { error  -> subject.onError(error) }
            )
    }

    override fun dispose() {
        _isActive = false
    }

    override fun today(context: Context): Observable<List<VEvent>> {
        scheduleNextRefresh(context)
        return subject.hide()
    }


    class CalendarRefreshWorker(
        context: Context,
        params: WorkerParameters
    ) : androidx.work.CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
            try {
                if (_instance == null) {
                    Log.e("iCalDataProvider", "No instance of iCalDataProvider found for refresh")
                    return Result.failure()
                }

                val timeProvider = _instance.timeProvider

                val now = timeProvider.now()
                if(now.hour < 18) {
                    _instance.refresh(now)
                } else {
                    _instance.refresh(now.plusDays(1))
                }

                _instance.scheduleNextRefresh(applicationContext)

                return Result.success()
            } catch (e: Exception) {
                Log.e("iCalDataProvider", "Error refreshing iCal data", e)
                return Result.failure()
            }
        }
    }


    fun scheduleNextRefresh(context: Context) {
        val now = timeProvider.now()
        val thisEvening = now.toLocalDate().atStartOfDay().plusHours(18)
        val nextMorning = now.toLocalDate().atStartOfDay().plusDays(1).plusHours(6)

        val delay = if(now < thisEvening) {
            Duration.between(now, thisEvening).toMinutes()
        } else {
            Duration.between(now, nextMorning).toMinutes()
        }

        val work = OneTimeWorkRequestBuilder<CalendarRefreshWorker>()
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .addTag("com.iceiony.visualcalendar")
            .build()

        WorkManager.getInstance(context).enqueue( work )
    }

    abstract fun getDaysEvents(now: LocalDateTime): List<VEvent>

}


fun DateStart.toTime(): String {
    return SimpleDateFormat("HH:mm").format(value as Date)
}

fun DateEnd.toTime(): String {
    return SimpleDateFormat("HH:mm").format(value as Date)
}
