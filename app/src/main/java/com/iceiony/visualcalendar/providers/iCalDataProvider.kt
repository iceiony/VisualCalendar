package com.iceiony.visualcalendar.providers

import android.content.Context
import android.annotation.SuppressLint
import androidx.work.PeriodicWorkRequestBuilder
import biweekly.Biweekly
import biweekly.component.VEvent
import com.iceiony.visualcalendar.BuildConfig
import com.iceiony.visualcalendar.SystemTimeProvider
import com.iceiony.visualcalendar.TimeProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.ReplaySubject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder

@SuppressLint("CheckResult")
class iCalDataProvider(
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val scheduler : Scheduler = Schedulers.io(),
    private val iCalUrl: String = BuildConfig.ICAL_DEBUG_URL
) : DataProvider {

    companion object {
        @Volatile
        private lateinit var _instance: iCalDataProvider
    }

    private val subject = ReplaySubject.create<List<VEvent>>(1)
    private val client = OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(30))
        .build()

    init {
        if (iCalUrl.isBlank()) {
            throw IllegalStateException("iCalUrl is not configured")
        }

        _instance = this

        refresh()
    }

    fun getTodaysEvents(now: LocalDateTime): List<VEvent> {
        val request = Request.Builder().url(iCalUrl).get().build()
        val response = client.newCall(request).execute()

        val body = response.body?.string() ?: throw IOException("Empty body")
        val calendar = Biweekly.parse(body).first()           // parse to ICalendar
            ?: throw IllegalStateException("Invalid ICS feed")

        val today = now.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC)
        val tomorrow = now.toLocalDate().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)

        return calendar.events.filter() { event ->
            event.dateStart.value.toInstant().isAfter(today) &&
            event.dateStart.value.toInstant().isBefore(tomorrow)
        }
    }

    override fun refresh() {
        val now = timeProvider.now()
        Observable
            .fromCallable{ getTodaysEvents(now) }
            .subscribeOn(scheduler)
            .observeOn(scheduler)
            .subscribe(
                { events -> subject.onNext(events) },
                { error  -> subject.onError(error) }
            )
    }

    override fun today(context: Context): Observable<List<VEvent>> {
        scheduleNextRefresh(context)
        return subject.hide()
    }


    class iCalRefreshWorker(
        context: Context,
        params: WorkerParameters
    ) : androidx.work.CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
            try {
                _instance.refresh()
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
        val evening = now.toLocalDate().atStartOfDay().plusHours(18)
        val delay = Duration.between(now, evening).toMinutes()

        var work = OneTimeWorkRequestBuilder<iCalRefreshWorker>()
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .addTag("com.iceiony.visualcalendar")
            .build()

        WorkManager.getInstance(context)
            .enqueue( work )
    }


}