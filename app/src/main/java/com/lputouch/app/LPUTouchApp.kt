package com.lputouch.app

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
        authRepository = AuthRepository(mobileApi, umsApi, sessionStore, db)
        studentRepository = StudentRepository(this, mobileApi, umsApi, happeningsApi, sessionStore, db, authRepository)

        // Restore the in-memory JWT cache so authenticated calls work after process restart.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            sessionStore.jwtToken.first()?.let { sessionStore.warmJwt(it) }
        }

        installCrashHandler()
    }

    /**
     * Safety-net crash handler: show a user-friendly toast AND forward to the
     * default handler so the system crash report (and any Crashlytics SDK) still
     * records the event. Without this, fatal exceptions would be silently eaten.
     */
    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log to logcat so `adb logcat` can still see it
            android.util.Log.e("LPUTouchPlus", "Uncaught exception", throwable)

            // Show a toast on the main thread so the user knows something happened
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    this,
                    "Something went wrong: ${throwable.message ?: "unexpected error"}",
                    Toast.LENGTH_LONG,
                ).show()
            }

            // Delegate to the system default handler (generates crash report, kills process)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient { ApiClient.baseHttpClient(sessionStore) }
            .build()
    }
}
