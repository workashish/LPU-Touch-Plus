package com.lputouch.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lputouch.app.notif.AnnouncementWorker
import com.lputouch.app.security.AppLockGate
import com.lputouch.app.ui.navigation.AppNavHost
import com.lputouch.app.ui.theme.LPUTouchPlusTheme
import java.util.concurrent.TimeUnit

// FragmentActivity (extends ComponentActivity) is required for BiometricPrompt.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LPUTouchApp
        scheduleAnnouncementWorker()
        com.lputouch.app.widget.TimetableWidgetReceiver.update(this)
        setContent {
            // Nullable initial: DataStore loads async — avoid flashing the login screen on restart.
            val loggedIn by app.sessionStore.loggedIn.collectAsState(initial = null)
            val darkMode by app.sessionStore.darkMode.collectAsState(initial = false)
            val biometricEnabled by app.sessionStore.biometricEnabled.collectAsState(initial = false)
            val pinEnabled by app.sessionStore.pinEnabled.collectAsState(initial = false)
            var unlocked by rememberSaveable { mutableStateOf(false) }

            // Only gate the app when the user actually enabled a lock (biometric or PIN).
            // After login there is no lock configured by default, so go straight in.
            val lockConfigured = biometricEnabled || pinEnabled
            val showGate = loggedIn == true && lockConfigured && !unlocked

            LPUTouchPlusTheme(darkTheme = darkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showGate) {
                        AppLockGate(
                            sessionStore = app.sessionStore,
                            onUnlocked = { unlocked = true },
                        )
                    } else if (loggedIn != null) {
                        AppNavHost(
                            loggedIn = loggedIn == true,
                            sessionStore = app.sessionStore,
                            authRepository = app.authRepository,
                            studentRepository = app.studentRepository,
                        )
                    }
                }
            }
        }
    }

    private fun scheduleAnnouncementWorker() {
        val request = PeriodicWorkRequestBuilder<AnnouncementWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "announcement_refresh",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
