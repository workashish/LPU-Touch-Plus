package com.lputouch.app.ui.timetable

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.TimetableItem
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import com.lputouch.app.util.rememberNetworkAvailability
import kotlinx.coroutines.launch
import java.util.Calendar

private val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    studentRepository: StudentRepository,
    onBack: () -> Unit,
) {
    var items by remember { mutableStateOf<List<TimetableItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val isOnline = rememberNetworkAvailability()

    // Default selected tab to today.
    // Calendar: 1=Sunday, 2=Monday, ..., 7=Saturday
    // We want:   0=Monday, 1=Tuesday, ..., 6=Sunday
    val initialDay = remember {
        val calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        when (calDay) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }.coerceIn(0, 6)
    
    var selectedTabIndex by remember { mutableIntStateOf(initialDay) }
    val scope = rememberCoroutineScope()

    suspend fun load(force: Boolean = false) {
        error = null
        try {
            val data = studentRepository.getTimetable(forceRefresh = if (!isOnline) false else force)
            items = data
            if (data.isEmpty() && !isOnline) error = "No internet connection"
        } catch (e: Exception) {
            error = if (!isOnline) "No internet connection" else (e.message ?: "Failed to load")
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        load()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timetable", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (!loading && error == null) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    dayNames.forEachIndexed { index, title ->
                        val isToday = index == initialDay
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                androidx.compose.foundation.layout.Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = title, 
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal 
                                    ) 
                                    if (isToday) {
                                        Spacer(Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            when {
                loading -> LoadingState(Modifier.fillMaxSize())

                error != null && items.isEmpty() -> ErrorState(
                    message = error!!,
                    onRetry = { scope.launch { loading = true; load(); loading = false } },
                    modifier = Modifier.fillMaxSize(),
                )

                items.isEmpty() -> EmptyState("No timetable available", Modifier.fillMaxSize())

                else -> {
                    val currentDayItems = items.filter { it.day == selectedTabIndex + 1 }.sortedBy { item ->
                        val timeStr = item.attendanceTime ?: ""
                        try {
                            val startPart = timeStr.substringBefore("-").trim()
                            val amPm = timeStr.substringAfterLast(" ").trim().uppercase()
                            
                            val parts = startPart.split(":")
                            var hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
                            val minute = parts.getOrNull(1)?.substringBefore(" ")?.toIntOrNull() ?: 0
                            
                            if (amPm.contains("PM") && hour < 12) hour += 12
                            if (amPm.contains("AM") && hour == 12) hour = 0
                            
                            hour * 60 + minute
                        } catch (e: Exception) {
                            0
                        }
                    }
                    
                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = {
                            scope.launch {
                                refreshing = true
                                load(true)
                                refreshing = false
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (currentDayItems.isEmpty()) {
                            EmptyState("No classes scheduled for this day", Modifier.fillMaxSize())
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(currentDayItems, key = { "${it.day}-${it.attendanceTime}-${it.courseCode}" }) { t ->
                                    TimetableCard(t)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableCard(t: TimetableItem) {
    val desc = t.description?.replace("\r", "")?.replace("\n", "")?.trim() ?: ""
    val isAvailableShortly = desc.contains("available Shortly", ignoreCase = true)
    val isMakeup = desc.contains("[MAKEUP]", ignoreCase = true)
    
    // Improved Regex to handle arbitrary spacing and newlines, adding missing colons!
    val courseCodeMatch = Regex("C:\\s*([^ /]+)").find(desc)?.groupValues?.get(1) ?: Regex("C:\\s*:?\\s*([^ /]+)").find(desc)?.groupValues?.get(1) ?: Regex("C:([^ /]+)").find(desc)?.groupValues?.get(1)
    val courseCodeFix = Regex("C:\\s*:?\\s*([^ /]+)").find(desc)?.groupValues?.get(1)
    val roomFix = Regex("R:\\s*:?\\s*([^ /]+)").find(desc)?.groupValues?.get(1)
    val sectionFix = Regex("S:\\s*:?\\s*([^ /]+)").find(desc)?.groupValues?.get(1)
    
    val courseCodeMatchFinal = courseCodeFix ?: courseCodeMatch
    val roomMatch = roomFix
    val sectionMatch = sectionFix
    
    val parts = desc.split("/")
    val type = parts.firstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: "Course"

    val displayTitle = t.courseName?.takeIf { it.isNotBlank() } 
        ?: t.subjectName?.takeIf { it.isNotBlank() } 
        ?: t.courseCode?.takeIf { it.isNotBlank() } 
        ?: courseCodeMatchFinal?.takeIf { it.isNotBlank() } 
        ?: if (isAvailableShortly) "Notice" else type
        
    val displaySubtitle = if (isAvailableShortly) desc
        else if (isMakeup) desc.removePrefix("[MAKEUP]").trim()
        else (type + (sectionMatch?.let { " • Sec $it" } ?: ""))
    val displayRoom = t.roomNo?.takeIf { it.isNotBlank() } ?: roomMatch
    val displayFaculty = t.facultyName?.takeIf { it.isNotBlank() }

    if (isAvailableShortly) {
        // Special Notice Card Design
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Info, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Schedule Update",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    } else {
        // Normal Class Card — clean vertical layout, works on all screen sizes
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                // Row 1: Time badge + Course title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Time badge
                    if (!t.attendanceTime.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = t.attendanceTime,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                    // Course name — takes remaining width
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }

                // Row 2: Section tag + Course code (if available)
                val codeOrSection = listOfNotNull(
                    courseCodeMatchFinal?.takeIf { it.isNotBlank() },
                    sectionMatch?.let { "Sec $it" },
                ).joinToString("  •  ")
                if (codeOrSection.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = codeOrSection,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Row 3: Room + Faculty (chip-style tags)
                val hasRoom = !displayRoom.isNullOrBlank()
                val hasFaculty = !displayFaculty.isNullOrBlank()
                if (hasRoom || hasFaculty) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        displayRoom?.takeIf { it.isNotBlank() }?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        displayFaculty?.takeIf { it.isNotBlank() }?.let {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false),
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
