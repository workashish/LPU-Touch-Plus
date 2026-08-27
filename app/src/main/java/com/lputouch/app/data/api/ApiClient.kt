package com.lputouch.app.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.lputouch.app.data.prefs.SessionStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    const val BASE_URL_MOBILE = "https://mobileapi.lpu.in/"
    const val BASE_URL_UMS = "https://ums.lpu.in/"
    const val BASE_URL_HAPPENINGS = "https://happenings.lpu.in/"

    private val gson: Gson = GsonBuilder().create()

    /** Patterns that indicate the server session has expired. */
    private val SESSION_EXPIRED_PATTERNS = listOf(
        "your session has expired",
        "session expired",
        "unauthorized",
        "token expired",
    )

    fun baseHttpClient(sessionStore: SessionStore): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request()
                val jwt = sessionStore.jwtSync()
                val builder = request.newBuilder().header("Content-Type", "application/json")
                if (!jwt.isNullOrEmpty()) {
                    builder.header("Authorization", "Bearer $jwt")
                }

                val response = chain.proceed(builder.build())

                // Only check text-based responses (skip images, PDFs, binary)
                val contentType = response.header("Content-Type") ?: ""
                if (contentType.contains("json", ignoreCase = true) ||
                    contentType.contains("text", ignoreCase = true)) {
                    val body = response.body ?: return@addInterceptor response
                    // Read only the first 4KB to detect session expiry without loading entire body
                    val peekSource = body.source().peek()
                    val peekBuffer = okio.Buffer()
                    val bytesToRead = minOf(4096L, peekSource.buffer.size)
                    peekSource.read(peekBuffer, bytesToRead)
                    val preview = peekBuffer.readString(Charsets.UTF_8)

                    val isExpired = SESSION_EXPIRED_PATTERNS.any { pattern ->
                        preview.contains(pattern, ignoreCase = true)
                    }
                    if (isExpired) {
                        throw IOException("SESSION_EXPIRED")
                    }
                }
                response
            }
            .build()
    }

    fun mobileApi(sessionStore: SessionStore): MobileApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL_MOBILE)
            .client(baseHttpClient(sessionStore))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(MobileApi::class.java)

    fun umsApi(sessionStore: SessionStore): UmsApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL_UMS)
            .client(baseHttpClient(sessionStore))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(UmsApi::class.java)

    fun happeningsApi(): HappeningsApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL_HAPPENINGS)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(HappeningsApi::class.java)
}
