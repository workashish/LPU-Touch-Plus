package com.lputouch.app.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.lputouch.app.data.prefs.SessionStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    const val BASE_URL_MOBILE = "https://mobileapi.lpu.in/"
    const val BASE_URL_UMS = "https://ums.lpu.in/"
    const val BASE_URL_HAPPENINGS = "https://happenings.lpu.in/"

    private val gson: Gson = GsonBuilder().create()

    private fun baseHttpClient(sessionStore: SessionStore): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
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
                chain.proceed(builder.build())
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
