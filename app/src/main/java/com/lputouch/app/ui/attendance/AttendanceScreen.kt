package com.lputouch.app.ui.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.AttendanceItem
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    studentRepository: StudentRepository,
    onBack: () -> Unit,
) {
    var items by remember { mutableStateOf<List<AttendanceItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load(force: Boolean = false) {
        error = null
        try {
            items = studentRepository.getAttendance(forceRefresh = force)
        } catch (e: Exception) {
            error = e.message ?: "Failed to load"
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
                title = { Text("Attendance") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        when {
            loading -> LoadingState(Modifier.padding(padding))

            error != null && items.isEmpty() -> ErrorState(
                message = error!!,
                onRetry = { scope.launch { loading = true; load(); loading = false } },
                modifier = Modifier.padding(padding),
            )

            items.isEmpty() -> EmptyState("No attendance data", Modifier.padding(padding))

            else -> PullToRefreshBox(
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
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items, key = { it.courseCode ?: "${it.courseName}-${it.attendanceDay}" }) { a ->
                        AttendanceCard(a)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceCard(a: AttendanceItem) {
    val total = a.totalAttendance?.replace("%", "")?.trim()?.toDoubleOrNull()
    val progress = ((total ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    val color = when {
        total == null -> MaterialTheme.colorScheme.primary
        total >= 75.0 -> MaterialTheme.colorScheme.primary
        total >= 60.0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = a.courseName ?: a.subjectName ?: a.courseCode ?: "Course",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
            )
            a.courseCode?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f),
                    color = color,
                )
                Spacer(Modifier.padding(start = 12.dp))
                Text(
                    text = a.totalAttendance ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                )
            }
            a.theoryAttendance?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text("Theory: $it", style = MaterialTheme.typography.labelSmall)
            }
            a.practicalAttendance?.takeIf { it.isNotBlank() }?.let {
                Text("Practical: $it", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
