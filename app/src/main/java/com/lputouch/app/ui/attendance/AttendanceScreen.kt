package com.lputouch.app.ui.attendance

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lputouch.app.util.rememberNetworkAvailability
import com.lputouch.app.data.api.dto.AttendanceItem
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import kotlinx.coroutines.launch
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    studentRepository: StudentRepository,
    onBack: () -> Unit,
    onViewDetail: ((courseCode: String) -> Unit)? = null,
) {
    var items by remember { mutableStateOf<List<AttendanceItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val isOnline = rememberNetworkAvailability()

    suspend fun load(force: Boolean = false) {
        error = null
        try {
            // Always load from cache first (works offline too).
            // forceRefresh=true will attempt network even if cache is fresh.
            val data = studentRepository.getAttendance(forceRefresh = if (!isOnline) false else force)
            items = data
            // Only show offline warning if we have no data at all
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
                title = { Text("Attendance", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Summary banner
                    item {
                        AttendanceSummaryBanner(items)
                    }

                    items(items, key = { it.courseCode ?: "${it.courseName}-${it.courseType}" }) { a ->
                        AttendanceCard(a, onClick = {
                            a.courseCode?.let { code -> onViewDetail?.invoke(code) }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceSummaryBanner(items: List<AttendanceItem>) {
    val validItems = items.filter { (it.totalDelv?.toDoubleOrNull() ?: 0.0) > 0 }
    val above = validItems.count { ((it.totalPerc ?: it.totalAttendance)?.replace("%", "")?.trim()?.toDoubleOrNull() ?: 0.0) >= 75.0 }
    val below = validItems.size - above
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SummaryChip("Above 75%", above.toString(), MaterialTheme.colorScheme.primary)
            SummaryChip("Below 75%", below.toString(), MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String, color: Color) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AttendanceCard(a: AttendanceItem, onClick: () -> Unit) {
    val totalStr = a.totalPerc ?: a.totalAttendance
    val total = totalStr?.replace("%", "")?.trim()?.toDoubleOrNull()
    val totalDelivered = a.totalDelv?.toDoubleOrNull() ?: 0.0
    val progress = ((total ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    
    val color = when {
        totalDelivered == 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        total == null -> MaterialTheme.colorScheme.primary
        total >= 75.0 -> MaterialTheme.colorScheme.primary
        total >= 60.0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    // Classes needed to reach 75%
    val classesNeeded = if (total != null && total < 75.0 && totalDelivered > 0) {
        val attended = a.totalAttd?.toDoubleOrNull() ?: (total * totalDelivered / 100.0)
        // We need (attended + x) / (totalDelivered + x) >= 0.75
        val needed = kotlin.math.ceil((0.75 * totalDelivered - attended) / 0.25).toInt().coerceAtLeast(0)
        needed
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = a.courseName ?: a.subjectName ?: a.courseCode ?: "Course",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                    )
                    a.courseCode?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    text = if (totalStr != null) "$totalStr%" else "—",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = color,
                trackColor = color.copy(alpha = 0.15f),
            )
            
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (a.totalDelv != null && a.totalAttd != null) {
                    Text("Attended: ${a.totalAttd} / ${a.totalDelv}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                a.courseType?.takeIf { it.isNotBlank() }?.let {
                    Text("Type: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!a.faculty.isNullOrBlank() || !a.room.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    a.faculty?.takeIf { it.isNotBlank() }?.let {
                        Text("Faculty: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    a.room?.takeIf { it.isNotBlank() }?.let {
                        Text("Room: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (classesNeeded != null && classesNeeded > 0) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Attend ~$classesNeeded more classes to reach 75%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
