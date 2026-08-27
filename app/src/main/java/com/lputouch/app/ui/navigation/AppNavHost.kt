package com.lputouch.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lputouch.app.data.prefs.SessionStore
import com.lputouch.app.data.repo.AuthRepository
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.login.LoginScreen
import com.lputouch.app.ui.main.MainScreen

object Routes {
    const val LOGIN = "login"
    const val MAIN = "main" // The host for post-login screens

    // Kept here so they are accessible from anywhere
    const val HOME = "home"
    const val MARKS = "marks"
    const val ATTENDANCE = "attendance"
    const val TIMETABLE = "timetable"
    const val ANNOUNCEMENTS = "announcements"
    const val ANNOUNCEMENT_DETAIL = "announcement/{announcementId}?tab={tab}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val MESSAGES = "messages"
    const val PLACEMENT = "placement"
    const val MENTOR_REMARKS = "mentor_remarks"
    const val RMS_QUERIES = "rms_queries"
    const val SCORES = "scores"
    const val RPL = "rpl"
    const val MAKEUP = "makeup"

    // ── New routes ────────────────────────────────────────────────────────────
    const val FEE = "fee"
    const val DOCUMENTS = "documents"
    const val SEATING_PLAN = "seating_plan"
    const val LIBRARY = "library"
    const val BUS_ROUTES = "bus_routes"
    const val EDU_REV = "edu_rev"
    const val HOSTEL_LEAVE = "hostel_leave"
    const val PHONE_DIRECTORY = "phone_directory"
    const val CALENDAR = "calendar"
    const val LEADERBOARD = "leaderboard"
    const val NEWS = "news"
    const val ATTENDANCE_DETAIL = "attendance_detail/{courseCode}"
}

@Composable
fun AppNavHost(
    loggedIn: Boolean,
    sessionStore: SessionStore,
    authRepository: AuthRepository,
    studentRepository: StudentRepository,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = if (loggedIn) Routes.MAIN else Routes.LOGIN,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                authRepository = authRepository,
                onLoggedIn = { navController.navigate(Routes.MAIN) { popUpTo(Routes.LOGIN) { inclusive = true } } },
            )
        }
        composable(Routes.MAIN) {
            MainScreen(
                sessionStore = sessionStore,
                authRepository = authRepository,
                studentRepository = studentRepository,
                rootNavController = navController,
            )
        }
    }
}
