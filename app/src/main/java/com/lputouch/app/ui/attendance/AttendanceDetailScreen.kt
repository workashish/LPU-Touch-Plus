package com.lputouch.app.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.AttendanceDetailItem
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import kotlinx.coroutines.launch

/** Check if a status code means "present" (attended class). */
private fun isPresentStatus(status: String?): Boolean {
    val s = status?.uppercase() ?: return false
    return s.startsWith("P") || s.contains("PRESENT") ||
           s.startsWith("D") || s.contains("DUTY") ||
           s == "O"
}

/** Check if a status code means "absent". */
private fun isAbsentStatus(status: String?): Boolean {
    val s = status?.uppercase() ?: return false
    return s.startsWith("A") || s.contains("ABSENT")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDetailScreen(
    studentRepository: StudentRepository,
    courseCode: String,
    onBack: () -> Unit,
) {
    var items by remember { mutableStateOf<List<AttendanceDetailItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load(force: Boolean = false) {
        error = null
        try {
            val result = studentRepository.getAttendanceDetail(courseCode)
            items = result
        } catch (e: Exception) {
            error = e.message ?: "Failed to load attendance detail"
        }
    }

    LaunchedEffect(courseCode) {
        loading = true
        load()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Attendance Detail", fontWeight = FontWeight.SemiBold)
                        Text(courseCode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
    ) { padding ->
        when {
            loading -> LoadingState(Modifier.padding(padding))
            error != null && items.isEmpty() -> ErrorState(
                message = error!!,
                onRetry = { scope.launch { loading = true; load(); loading = false } },
                modifier = Modifier.padding(padding),
            )
            items.isEmpty() -> EmptyState("No attendance detail available", Modifier.padding(padding))
            else -> {
                // Correct counting: only explicit P/D/O = present, A = absent, everything else is "other"
                val present = items.count { isPresentStatus(it.status) }
                val absent = items.count { isAbsentStatus(it.status) }
                val other = items.size - present - absent

                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = {
                        scope.launch {
                            refreshing = true
                            load(true)
                            refreshing = false
                        }
                    },
                    modifier = Modifier.padding(padding),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Row(
                                    Modifier.padding(20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                ) {
                                    Column {
                                        Text(items.size.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        Text("Total", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Column {
                                        Text(present.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("Present", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Column {
                                        Text(absent.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                        Text("Absent", style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (other > 0) {
                                        Column {
                                            Text(other.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                            Text("Other", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }

                        items(items) { att ->
                            val isPresent = isPresentStatus(att.status)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(12.dp).background(
                                            when {
                                                isPresent -> Color(0xFF2E7D32)
                                                isAbsentStatus(att.status) -> Color(0xFFC62828)
                                                else -> MaterialTheme.colorScheme.tertiary
                                            },
                                            CircleShape
                                        )
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(att.date ?: "—", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        att.time?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        att.facultyName?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    }
                                    Text(
                                        text = att.status ?: "—",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = when {
                                            isPresent -> Color(0xFF2E7D32)
                                            isAbsentStatus(att.status) -> Color(0xFFC62828)
                                            else -> MaterialTheme.colorScheme.tertiary
                                        },
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
