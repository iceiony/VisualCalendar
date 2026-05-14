package com.iceiony.visualcalendar.testutil

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class TestInterceptor(
    val path: String,
    val body: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        return when {
            request.url.toString().contains(path) -> {
                val responseBody =  body.trimIndent().toResponseBody("application/json".toMediaType())

                // Return a mock response
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseBody)
                    .build()
            }

            else -> chain.proceed(request) // All other requests pass through normally
        }
    }
}