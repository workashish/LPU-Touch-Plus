package com.lputouch.app.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.lputouch.app.ui.attendance.AttendanceScreen
import com.lputouch.app.ui.home.HomeScreen
import com.lputouch.app.ui.marks.MarksScreen
import com.lputouch.app.ui.messages.MessagesScreen
import com.lputouch.app.ui.more.AptitudeScoresScreen
import com.lputouch.app.ui.more.MakeupScreen
import com.lputouch.app.ui.more.MentorRemarksScreen
import com.lputouch.app.ui.more.RmsQueriesScreen
import com.lputouch.app.ui.more.RplScreen
import com.lputouch.app.ui.navigation.Routes
import com.lputouch.app.ui.placement.PlacementScreen
import com.lputouch.app.ui.profile.ProfileScreen
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
    BottomNavItem(Routes.TIMETABLE, "Timetable", Icons.Filled.CalendarMonth),
    BottomNavItem(Routes.ANNOUNCEMENTS, "News", Icons.Filled.Campaign),
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
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = selected,
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
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
            
            // Sub-screens pushed onto the stack hiding the bottom bar
            composable(Routes.MARKS) { MarksScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.ATTENDANCE) { AttendanceScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS) { SettingsScreen(sessionStore, onBack = { navController.popBackStack() }) }
            composable(Routes.MESSAGES) { MessagesScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.PLACEMENT) { PlacementScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.MENTOR_REMARKS) { MentorRemarksScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.RMS_QUERIES) { RmsQueriesScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.SCORES) { AptitudeScoresScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.RPL) { RplScreen(studentRepository, onBack = { navController.popBackStack() }) }
            composable(Routes.MAKEUP) { MakeupScreen(studentRepository, onBack = { navController.popBackStack() }) }
        }
    }
}
