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
import com.iceiony.visualcalendar.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.util.Date

interface DataProvider {
    fun today(): SharedFlow<List<VEvent>>
    suspend fun refresh(now: LocalDateTime)

    suspend fun calendars(): Map<String, String>

    suspend fun getMainCalendar() : String?
    fun setMainCalendar(calendarId: String)
    fun destroy()
}

abstract class ScheduledDataProvider(
    val workManager: WorkManager,
    val timeProvider: TimeProvider,
    val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    val client: OkHttpClient = OkHttpClient.Builder() .callTimeout(Duration.ofSeconds(30)) .build()
) : DataProvider {

    protected val events = MutableSharedFlow<List<VEvent>>(replay = 1)
    private var _isActive: Boolean
    private var pendingWork: java.util.UUID? = null

    companion object {
        @Volatile
        private var _instance: ScheduledDataProvider? = null

    }

    init {
        if (_instance != null) {
            _instance?.destroy()
        }

        _instance = this
        _isActive = false
    }

    override suspend fun refresh(now: LocalDateTime) {
        try {
            events.emit(getDaysEvents(now))
        } catch (e: Exception) {
            Log.e("ScheduledDataProvider", "Error refreshing events", e)
        }
    }

    override fun today(): SharedFlow<List<VEvent>> {

        if (!_isActive) {
            scope.launch {
                _isActive = true

                scheduleNextRefresh(workManager)

                val now = timeProvider.now()
                if (now.hour < 18) {
                    refresh(now)
                } else {
                    refresh(now.plusDays(1))
                }
            }
        }

        return events
    }

    class CalendarRefreshWorker(
        context: Context,
        params: WorkerParameters
    ) : androidx.work.CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
            try {
                if (_instance == null || _instance?._isActive == false) {
                    Log.e("iCalDataProvider", "No instance of DataProvider found for refresh")
                    return Result.failure()
                }

                _instance?.scheduleNextRefresh(
                    WorkManager.getInstance(applicationContext)
                )

                val timeProvider = _instance?.timeProvider ?: return Result.failure()

                val now = timeProvider.now()
                if(now.hour < 18) {
                    _instance?.refresh(now)
                } else {
                    _instance?.refresh(now.plusDays(1))
                }

                return Result.success()
            } catch (e: Exception) {
                Log.e("iCalDataProvider", "Error refreshing iCal data", e)
                return Result.failure()
            }
        }
    }


    fun scheduleNextRefresh(workManager: WorkManager) {
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

        workManager.enqueue( work )

        pendingWork = work.id
    }

    abstract suspend fun getDaysEvents(now: LocalDateTime): List<VEvent>

    override fun destroy(){
        _isActive = false
        pendingWork?.let {
            workManager.cancelWorkById(it)
        }
    }
}


fun DateStart.toTime(): String {
    return SimpleDateFormat("HH:mm").format(value as Date)
}

fun DateEnd.toTime(): String {
    return SimpleDateFormat("HH:mm").format(value as Date)
}
