package com.lputouch.app

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.lputouch.app.data.api.ApiClient
import com.lputouch.app.data.db.AppDatabase
import com.lputouch.app.data.prefs.SecureStore
import com.lputouch.app.data.prefs.SessionStore
import com.lputouch.app.data.repo.AuthRepository
import com.lputouch.app.data.repo.StudentRepository
import coil.ImageLoader
import coil.ImageLoaderFactory

class LPUTouchApp : Application(), ImageLoaderFactory {

    lateinit var sessionStore: SessionStore
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var studentRepository: StudentRepository
        private set

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this, SecureStore(this))
        val db = AppDatabase.get(this)
        val mobileApi = ApiClient.mobileApi(sessionStore)
        val umsApi = ApiClient.umsApi(sessionStore)
        val happeningsApi = ApiClient.happeningsApi()
        authRepository = AuthRepository(mobileApi, umsApi, sessionStore)
        studentRepository = StudentRepository(this, mobileApi, umsApi, happeningsApi, sessionStore, db, authRepository)

        // Restore the in-memory JWT cache so authenticated calls work after process restart.
        runBlocking { sessionStore.jwtToken.first() }?.let { sessionStore.warmJwt(it) }

        installCrashHandler()
    }

    /**
     * Never let an unexpected exception kill the app silently — show a toast
     * and keep running. Network errors are already handled per-request;
     * this is a safety net for anything we missed.
     */
    private fun installCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            throwable.printStackTrace()
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    this,
                    "Something went wrong: ${throwable.message ?: "unexpected error"}",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient { ApiClient.baseHttpClient(sessionStore) }
            .build()
    }
}
