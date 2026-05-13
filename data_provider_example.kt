package com.iceiony.visualcalendar.providers

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import biweekly.component.VEvent
import com.iceiony.visualcalendar.SystemTimeProvider
import com.iceiony.visualcalendar.TimeProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.ReplaySubject
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.TimeUnit

@SuppressLint("CheckResult")
class GoogleCalendarDataProvider(
    private val appContext: Context,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val scheduler: Scheduler = Schedulers.io(),
    private val authManager: GoogleAuthManager = GoogleAuthManager(appContext),
) : DataProvider {

    companion object {
        @Volatile
        private lateinit var _instance: GoogleCalendarDataProvider
    }

    private val subject = ReplaySubject.create<List<VEvent>>(1)
    private val client = OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(30))
        .build()

    init {
        _instance = this
        val now = timeProvider.now()
        if (now.hour < 18) refresh(now) else refresh(now.plusDays(1))
    }

    fun getDaysEvents(now: LocalDateTime): List<VEvent> {
        val token = authManager.getValidAccessToken()
        val calendarId = authManager.getCalendarId() ?: throw Exception("No calendar selected")
        val zone = ZoneId.systemDefault()
        val dayStart = now.toLocalDate().atStartOfDay(zone).toInstant()
        val dayEnd = now.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()

        val url = "https://www.googleapis.com/calendar/v3/calendars/" +
            "${URLEncoder.encode(calendarId, "UTF-8")}/events" +
            "?timeMin=${URLEncoder.encode(dayStart.toString(), "UTF-8")}" +
            "&timeMax=${URLEncoder.encode(dayEnd.toString(), "UTF-8")}" +
            "&singleEvents=true&orderBy=startTime&maxResults=50"

        val response = client.newCall(
            Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
        ).execute()

        val body = response.body?.string() ?: throw Exception("Empty calendar response")
        val items = JSONObject(body).getJSONArray("items")

        return (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            VEvent().apply {
                setSummary(item.optString("summary", "(No Title)"))
                setDateStart(parseDateTime(item.getJSONObject("start")))
                setDateEnd(parseDateTime(item.getJSONObject("end")))
                extractImageUrl(item)?.let { addExperimentalProperty("X-IMAGE-URL", it) }
            }
        }
    }

    private fun parseDateTime(timeObj: JSONObject): Date {
        val dateStr = timeObj.optString("dateTime").ifEmpty { timeObj.getString("date") }
        return try {
            Date.from(Instant.parse(dateStr))
        } catch (e: Exception) {
            Date.from(java.time.LocalDate.parse(dateStr).atStartOfDay(ZoneId.systemDefault()).toInstant())
        }
    }

    private fun extractImageUrl(item: JSONObject): String? {
        if (item.has("attachments")) {
            val attachments = item.getJSONArray("attachments")
            for (i in 0 until attachments.length()) {
                val attachment = attachments.getJSONObject(i)
                if (attachment.optString("mimeType").startsWith("image/")) {
                    return attachment.optString("fileUrl").takeIf { it.isNotEmpty() }
                }
            }
        }
        val description = item.optString("description")
        return Regex("""\[image](https?://\S+)""").find(description)?.groupValues?.get(1)
    }

    override fun refresh(now: LocalDateTime) {
        Observable
            .fromCallable { getDaysEvents(now) }
            .subscribeOn(scheduler)
            .observeOn(scheduler)
            .subscribe(
                { events -> subject.onNext(events) },
                { error ->
                    Log.e("GoogleCalendarProvider", "Error fetching events", error)
                    subject.onError(error)
                }
            )
    }

    override fun today(context: Context): Observable<List<VEvent>> {
        scheduleNextRefresh(context)
        return subject.hide()
    }

    fun scheduleNextRefresh(context: Context) {
        val now = timeProvider.now()
        val thisEvening = now.toLocalDate().atStartOfDay().plusHours(18)
        val nextMorning = now.toLocalDate().atStartOfDay().plusDays(1).plusHours(6)

        val delayMinutes = if (now < thisEvening) {
            Duration.between(now, thisEvening).toMinutes()
        } else {
            Duration.between(now, nextMorning).toMinutes()
        }

        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<GoogleCalendarRefreshWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .addTag("com.iceiony.visualcalendar")
                .build()
        )
    }

    class GoogleCalendarRefreshWorker(
        context: Context,
        params: WorkerParameters,
    ) : androidx.work.CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
            return try {
                val now = _instance.timeProvider.now()
                if (now.hour < 18) _instance.refresh(now) else _instance.refresh(now.plusDays(1))
                _instance.scheduleNextRefresh(applicationContext)
                Result.success()
            } catch (e: Exception) {
                Log.e("GoogleCalendarProvider", "Error in refresh worker", e)
                Result.failure()
            }
        }
    }
}
