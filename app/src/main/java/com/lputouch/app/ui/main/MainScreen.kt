package com.lputouch.app.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lputouch.app.data.prefs.SessionStore
import com.lputouch.app.data.repo.AuthRepository
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.announcements.AnnouncementDetailScreen
import com.lputouch.app.ui.announcements.AnnouncementsScreen
import com.lputouch.app.ui.attendance.AttendanceDetailScreen
import com.lputouch.app.ui.attendance.AttendanceScreen
import com.lputouch.app.ui.bus.BusRoutesScreen
import com.lputouch.app.ui.calendar.CalendarScreen
import com.lputouch.app.ui.documents.DocumentsScreen
import com.lputouch.app.ui.edurev.EduRevScreen
import com.lputouch.app.ui.fee.FeeScreen
import com.lputouch.app.ui.home.HomeScreen
import com.lputouch.app.ui.hostel.HostelLeaveScreen
import com.lputouch.app.ui.leaderboard.LeaderboardScreen
import com.lputouch.app.ui.library.LibraryScreen
import com.lputouch.app.ui.marks.MarksScreen
import com.lputouch.app.ui.messages.MessagesScreen
import com.lputouch.app.ui.more.AptitudeScoresScreen
import com.lputouch.app.ui.more.MakeupScreen
import com.lputouch.app.ui.more.MentorRemarksScreen
import com.lputouch.app.ui.more.RmsQueriesScreen
import com.lputouch.app.ui.more.RplScreen
import com.lputouch.app.ui.navigation.Routes
import com.lputouch.app.ui.news.NewsScreen
import com.lputouch.app.ui.phonedir.PhoneDirectoryScreen
import com.lputouch.app.ui.placement.PlacementScreen
import com.lputouch.app.ui.profile.ProfileScreen
import com.lputouch.app.ui.seating.SeatingPlanScreen
import com.lputouch.app.ui.settings.SettingsScreen
import com.lputouch.app.ui.timetable.TimetableScreen
import kotlinx.coroutines.launch

private data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomNavItem(Routes.TIMETABLE, "Timetable", Icons.Filled.Assignment),
    BottomNavItem(Routes.ANNOUNCEMENTS, "Alerts", Icons.Filled.Campaign),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.Person)
)

@Composable
fun MainScreen(
    sessionStore: SessionStore,
    authRepository: AuthRepository,
    studentRepository: StudentRepository,
    rootNavController: NavHostController,
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .shadow(16.dp, RoundedCornerShape(32.dp))
                        .clip(RoundedCornerShape(32.dp))
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.title) },
                                label = { Text(item.title, style = MaterialTheme.typography.labelMedium) },
                                selected = selected,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    studentRepository = studentRepository,
                    sessionStore = sessionStore,
                    onOpen = { route -> navController.navigate(route) },
                    onLogout = {
                        scope.launch {
                            authRepository.logout()
                            rootNavController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                        }
                    },
                )
            }
            composable(Routes.TIMETABLE) { TimetableScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.ANNOUNCEMENTS) {
                AnnouncementsScreen(
                    studentRepository = studentRepository,
                    onBack = { navController.popBackStack() },
                    onOpen = { a ->
                        navController.navigate("announcement/${a.announcementId}?tab=${a.tab ?: "Online"}")
                    },
                )
            }
            composable(
                route = Routes.ANNOUNCEMENT_DETAIL,
                arguments = listOf(
                    navArgument("announcementId") { type = NavType.StringType },
                    navArgument("tab") { type = NavType.StringType; defaultValue = "Online" },
                ),
            ) { entry ->
                val id = entry.arguments?.getString("announcementId") ?: ""
                val tab = entry.arguments?.getString("tab") ?: "Online"
                AnnouncementDetailScreen(id, tab, studentRepository, onBack = { navController.popBackStack() })
            }
            composable(Routes.PROFILE) { ProfileScreen(studentRepository, onBack = { navController.popBackStack() }) }

            // Sub-screens (push to back stack, hides bottom bar)
            composable(Routes.MARKS) { MarksScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.ATTENDANCE) {
                AttendanceScreen(
                    studentRepository = studentRepository,
                    onBack = { navController.popBackStack() },
                    onViewDetail = { code -> navController.navigate("attendance_detail/$code") },
                )
            }
            composable(
                route = Routes.ATTENDANCE_DETAIL,
                arguments = listOf(navArgument("courseCode") { type = NavType.StringType }),
            ) { entry ->
                val code = entry.arguments?.getString("courseCode") ?: ""
                AttendanceDetailScreen(
                    studentRepository = studentRepository,
                    courseCode = code,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) { SettingsScreen(sessionStore, onClearCache = { studentRepository.clearAllCache() }, onBack = { navController.popBackStack() }) }
            composable(Routes.MESSAGES) { MessagesScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.PLACEMENT) { PlacementScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.MENTOR_REMARKS) { MentorRemarksScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.RMS_QUERIES) { RmsQueriesScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.SCORES) { AptitudeScoresScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.RPL) { RplScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.MAKEUP) { MakeupScreen(studentRepository, onBack = { navController.popBackStack() }) }

            // ── New screens ───────────────────────────────────────────────────
            composable(Routes.FEE) { FeeScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.DOCUMENTS) { DocumentsScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.SEATING_PLAN) { SeatingPlanScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.LIBRARY) { LibraryScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.BUS_ROUTES) { BusRoutesScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.EDU_REV) { EduRevScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.HOSTEL_LEAVE) { HostelLeaveScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.PHONE_DIRECTORY) { PhoneDirectoryScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.CALENDAR) { CalendarScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.LEADERBOARD) { LeaderboardScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.NEWS) { NewsScreen(studentRepository, onBack = { navController.popBackStack() }) }
        }
    }
}
