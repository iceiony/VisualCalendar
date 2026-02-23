package com.iceiony.visualcalendar.providers

import android.content.Context
import android.annotation.SuppressLint
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
import biweekly.util.ICalDate
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.TimeZone

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

        val now = timeProvider.now()
        if (now.hour < 18) {
            refresh(now)
        } else {
            refresh(now.plusDays(1))
        }
    }

    fun getDaysEvents(now: LocalDateTime): List<VEvent> {
        val request = Request.Builder().url(iCalUrl).get().build()
        val response = client.newCall(request).execute()

        val body = response.body?.string() ?: throw IOException("Empty body")
        val calendar = Biweekly.parse(body).first()           // parse to ICalendar
            ?: throw IllegalStateException("Invalid ICS feed")

        val today = now
            .toLocalDate()
            .atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()

        val tomorrow = now
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()

        val oneOffEvents =  calendar.events.filter() { event ->
            val start = event.dateStart.value.toInstant()
            start.isAfter(today) && start.isBefore(tomorrow)
        }

        val reoccurringEvents = calendar.events.filter() {
            it.recurrenceRule != null
        }.map { event ->
            val nextDates = event.getDateIterator(TimeZone.getDefault())

            while(nextDates.hasNext()) {
                val date = nextDates.next()
                if(date.toInstant().isBefore(today)) continue
                if(date.toInstant().isAfter(tomorrow)) break

                event.setDateEnd( Date(date.time + (event.dateEnd.value.time - event.dateStart.value.time)) )
                event.setDateStart(date)

                return@map event
            }
            return@map null
        }.filterNotNull()

        return  (oneOffEvents + reoccurringEvents).sortedBy { it.dateStart.value }
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

        val work = OneTimeWorkRequestBuilder<iCalRefreshWorker>()
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .addTag("com.iceiony.visualcalendar")
            .build()

        WorkManager.getInstance(context).enqueue( work )
    }


}