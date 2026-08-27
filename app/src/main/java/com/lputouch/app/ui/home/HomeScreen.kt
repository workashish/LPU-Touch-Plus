package com.lputouch.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.TimetableItem
import com.lputouch.app.data.prefs.SessionStore
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.navigation.Routes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

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
    var studentName by remember { mutableStateOf("Student") }
    var program by remember { mutableStateOf("") }
    var cgpa by remember { mutableStateOf<String?>(null) }
    var todayClasses by remember { mutableStateOf<List<TimetableItem>>(emptyList()) }
    var isLoadingClasses by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val saved = sessionStore.studentName.first()
        if (!saved.isNullOrBlank()) studentName = saved
        
        launch {
            val profile = studentRepository.getProfile()
            if (profile != null && profile.error.isNullOrBlank()) {
                profile.studentName?.takeIf { it.isNotBlank() }?.let { studentName = it }
                profile.programName?.let { program = it }
                profile.cgpa?.let { cgpa = it }
                sessionStore.saveProfile(
                    name = profile.studentName ?: studentName,
                    program = profile.programName ?: "",
                )
            }
        }
        
        launch {
            try {
                val timetable = studentRepository.getTimetable(forceRefresh = false)
                val cal = Calendar.getInstance()
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val appDay = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
                todayClasses = timetable.filter { it.day == appDay }.sortedBy { it.attendanceTime }
            } catch (e: Exception) {
                // Ignore failure for dashboard widget
            } finally {
                isLoadingClasses = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                actions = {
                    IconButton(onClick = { onLogout() }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Logout")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                WelcomeCard(name = studentName, program = program, cgpa = cgpa)
            }
            
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
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                        Text("Loading classes...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (todayClasses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "No classes scheduled for today! 🎉",
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(260.dp).padding(horizontal = 16.dp), // Fixed height to nest grid inside column (or calculate)
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
private fun WelcomeCard(name: String, program: String, cgpa: String?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Hi, $name",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (program.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = program,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (cgpa != null) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "CGPA $cgpa",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ClassWidgetCard(t: TimetableItem) {
    Card(
        modifier = Modifier.width(220.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = t.attendanceTime ?: "—",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.weight(1f))
                t.roomNo?.takeIf { it.isNotBlank() }?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(2.dp))
                        Text(it, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = t.courseName ?: t.subjectName ?: t.courseCode ?: "Course",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            t.facultyName?.takeIf { it.isNotBlank() }?.let {
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
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tile.icon, contentDescription = null, tint = tile.color, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = tile.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
