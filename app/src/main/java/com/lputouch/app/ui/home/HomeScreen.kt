package com.lputouch.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.StudentBasicInfo
import com.lputouch.app.data.api.dto.TimetableItem
import com.lputouch.app.data.prefs.SessionStore
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.navigation.Routes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar

/** Robust check for fee-active status. Handles "0", "false", "No", null, blank. */
private fun isFeeActive(isFee: String?): Boolean {
    if (isFee.isNullOrBlank()) return false
    val v = isFee.trim().lowercase()
    return v != "0" && v != "false" && v != "no" && v != "nil"
}

private fun parseTimeToMinutes(timeStr: String?): Int {
    if (timeStr.isNullOrBlank()) return 0
    try {
        val isPm = timeStr.contains("PM", ignoreCase = true)
        val startStr = timeStr.split("-").firstOrNull()?.trim() ?: return 0
        val parts = startStr.split(":")
        var hour = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val min = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        if (hour == 12) hour = 0
        return (hour + if (isPm) 12 else 0) * 60 + min
    } catch (e: Exception) {
        return 0
    }
}

private data class Tile(val route: String, val title: String, val icon: ImageVector, val color: Color)

private val quickLinks = listOf(
    Tile(Routes.MARKS, "Marks", Icons.Filled.School, Color(0xFF1565C0)),
    Tile(Routes.ATTENDANCE, "Attendance", Icons.Filled.Assignment, Color(0xFF2E7D32)),
    Tile(Routes.MESSAGES, "Messages", Icons.Filled.Email, Color(0xFF00838F)),
    Tile(Routes.PLACEMENT, "Placement", Icons.Filled.Work, Color(0xFFF57C00)),
    Tile(Routes.MENTOR_REMARKS, "Mentor", Icons.Filled.Groups, Color(0xFF5E35B1)),
    Tile(Routes.RMS_QUERIES, "Queries", Icons.Filled.MarkEmailRead, Color(0xFFC62828)),
    Tile(Routes.SCORES, "Scores", Icons.Filled.Star, Color(0xFF283593)),
    Tile(Routes.RPL, "RPL", Icons.Filled.Place, Color(0xFF00695C)),
    Tile(Routes.MAKEUP, "Makeup", Icons.Filled.Psychology, Color(0xFF4E342E)),
    Tile(Routes.FEE, "Fee", Icons.Filled.AccountBalance, Color(0xFF6A1B9A)),
    Tile(Routes.DOCUMENTS, "Documents", Icons.Filled.Folder, Color(0xFF0277BD)),
    Tile(Routes.SEATING_PLAN, "Seating", Icons.Filled.EventSeat, Color(0xFF558B2F)),
    Tile(Routes.LIBRARY, "Library", Icons.Filled.MenuBook, Color(0xFF4E342E)),
    Tile(Routes.BUS_ROUTES, "Bus", Icons.Filled.DirectionsBus, Color(0xFF00695C)),
    Tile(Routes.EDU_REV, "EduRev", Icons.Filled.PlayLesson, Color(0xFF1565C0)),
    Tile(Routes.HOSTEL_LEAVE, "Hostel", Icons.Filled.Hotel, Color(0xFF37474F)),
    Tile(Routes.PHONE_DIRECTORY, "Directory", Icons.Filled.Contacts, Color(0xFF0288D1)),
    Tile(Routes.LEADERBOARD, "Leaderboard", Icons.Filled.Leaderboard, Color(0xFFAD1457)),
    Tile(Routes.CALENDAR, "Calendar", Icons.Filled.CalendarToday, Color(0xFF1B5E20)),
    Tile(Routes.NEWS, "News", Icons.Filled.Newspaper, Color(0xFFE65100)),
    Tile(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Color(0xFF455A64)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    studentRepository: StudentRepository,
    sessionStore: SessionStore,
    onOpen: (String) -> Unit,
    onLogout: () -> Unit,
) {
    var profile by remember { mutableStateOf<StudentBasicInfo?>(null) }
    var studentName by remember { mutableStateOf("Student") }
    var todayClasses by remember { mutableStateOf<List<TimetableItem>>(emptyList()) }
    var isLoadingClasses by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val saved = sessionStore.studentName.first()
        if (!saved.isNullOrBlank()) studentName = saved

        launch {
            val p = studentRepository.getStudentBasicInfo()
            if (p != null && p.error.isNullOrBlank()) {
                profile = p
                p.studentName?.takeIf { it.isNotBlank() }?.let { studentName = it }
                sessionStore.saveProfile(
                    name = p.studentName ?: studentName,
                    program = p.programName ?: "",
                )
            }
        }

        launch {
            try {
                val timetable = studentRepository.getTimetable(forceRefresh = false)
                val cal = Calendar.getInstance()
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val appDay = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
                todayClasses = timetable.filter { it.day == appDay }.sortedBy { parseTimeToMinutes(it.attendanceTime) }
            } catch (e: Exception) {
                // Ignore failure for dashboard widget
            } finally {
                isLoadingClasses = false
            }
        }
    }

    Scaffold(
        topBar = {}

    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            // Hero Header
            item {
                HeroHeader(profile = profile, name = studentName, onLogout = onLogout)
            }

            // Fee alert banner
            val p = profile
            if (p != null && isFeeActive(p.isFee) && !p.feeText.isNullOrBlank()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    FeeAlertBanner(p.feeText ?: "", onClick = { onOpen(Routes.FEE) })
                }
            }

            // Birthday banner
            if (p?.isBirthdayToday == true) {
                item {
                    Spacer(Modifier.height(12.dp))
                    BirthdayBanner(p.studentName ?: "")
                }
            }

            // Stats row
            if (p != null) {
                item {
                    Spacer(Modifier.height(16.dp))
                    StatsRow(profile = p)
                }
            }

            // Today's classes
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Today's Classes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(12.dp))

                if (isLoadingClasses) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (todayClasses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "No classes scheduled for today!",
                                modifier = Modifier.padding(20.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(todayClasses) { t ->
                            ClassWidgetCard(t)
                        }
                    }
                }
            }

            // Quick Links
            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    text = "Quick Links",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(12.dp))
            }

            item {
                // Calculate grid height dynamically: 4 items per row, ~88dp per row
                val rows = (quickLinks.size + 3) / 4
                val gridHeight = (rows * 88).dp
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .heightIn(min = gridHeight)
                        .padding(horizontal = 16.dp),
                    userScrollEnabled = false,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickLinks) { tile ->
                        SmallTileCard(tile, onClick = { onOpen(tile.route) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroHeader(profile: StudentBasicInfo?, name: String, onLogout: () -> Unit) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentHour) {
        in 0..11 -> "Good Morning,"
        in 12..16 -> "Good Afternoon,"
        else -> "Good Evening,"
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        com.lputouch.app.ui.theme.GradientStart,
                        com.lputouch.app.ui.theme.GradientEnd
                    )
                )
            )
            .padding(top = 48.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                profile?.programName?.let { prog ->
                    if (prog.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = prog.substringAfter("L:").substringBefore("(").trim().ifBlank { prog },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    }
                }
                
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                }
            }
        }
    }

@Composable
private fun StatPill(text: String) {
    Box(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FeeAlertBanner(message: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE65100),
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color(0xFFF57C00))
        }
    }
}

@Composable
private fun BirthdayBanner(name: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE4EC)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Cake, contentDescription = "Birthday", tint = Color(0xFFC2185B))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Happy Birthday!", fontWeight = FontWeight.Bold, color = Color(0xFFC2185B))
                Text("Have a wonderful day, ${name.substringBefore(" ")}!", style = MaterialTheme.typography.bodySmall, color = Color(0xFFAD1457))
            }
        }
    }
}

@Composable
private fun StatsRow(profile: StudentBasicInfo) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        profile.aggAttendance?.takeIf { it.isNotBlank() }?.let {
            item { StatChipItem("Attendance", it, Icons.Filled.Assignment, Color(0xFF2E7D32)) }
        }
        profile.announcementCount?.takeIf { it.isNotBlank() && it != "0" }?.let {
            item { StatChipItem("New Alerts", it, Icons.Filled.Campaign, Color(0xFFE65100)) }
        }
        profile.myMessagesCount?.takeIf { it.isNotBlank() && it != "0" }?.let {
            item { StatChipItem("Messages", it, Icons.Filled.Email, Color(0xFF00838F)) }
        }
        profile.seatingPlanExamCount?.takeIf { it.isNotBlank() && it != "0" }?.let {
            item { StatChipItem("Exams", it, Icons.Filled.EventSeat, Color(0xFF6A1B9A)) }
        }
        profile.currentBalance?.takeIf { it.isNotBlank() && it.lowercase() != "nil" }?.let {
            item { StatChipItem("Balance", it, Icons.Filled.AccountBalance, Color(0xFFC62828)) }
        }
    }
}

@Composable
private fun StatChipItem(label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Column {
                Text(value, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun ClassWidgetCard(t: TimetableItem) {
    val desc = t.description?.replace("\r", "")?.replace("\n", "")?.trim() ?: ""
    val isAvailableShortly = desc.contains("available Shortly", ignoreCase = true)
    
    val courseCodeFix = Regex("C:\\s*:?\\s*([^ /]+)").find(desc)?.groupValues?.get(1)
    val roomFix = Regex("R:\\s*:?\\s*([^ /]+)").find(desc)?.groupValues?.get(1)
    
    val parts = desc.split("/")
    val type = parts.firstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: "Course"

    val displayTitle = t.courseName?.takeIf { it.isNotBlank() } 
        ?: t.subjectName?.takeIf { it.isNotBlank() } 
        ?: t.courseCode?.takeIf { it.isNotBlank() } 
        ?: courseCodeFix?.takeIf { it.isNotBlank() } 
        ?: if (isAvailableShortly) "Notice" else type
    val displayRoom = t.roomNo?.takeIf { it.isNotBlank() } ?: roomFix
    val displayFaculty = t.facultyName?.takeIf { it.isNotBlank() }
    
    Card(
        modifier = Modifier.width(220.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailableShortly) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isAvailableShortly && !t.attendanceTime.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = t.attendanceTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isAvailableShortly) {
                    Icon(
                        Icons.Filled.Info, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                displayRoom?.takeIf { it.isNotBlank() }?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(2.dp))
                        Text(it, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isAvailableShortly) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            if (isAvailableShortly) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(8.dp))
            displayFaculty?.takeIf { it.isNotBlank() }?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SmallTileCard(tile: Tile, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(tile.color.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tile.icon, contentDescription = null, tint = tile.color, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = tile.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
