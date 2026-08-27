package com.lputouch.app.ui.seating

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.SeatingPlanItem
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatingPlanScreen(studentRepository: StudentRepository, onBack: () -> Unit) {
    var items by remember { mutableStateOf<List<SeatingPlanItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load(force: Boolean = false) {
        error = null
        try {
            items = studentRepository.getSeatingPlan()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load seating plan"
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
                title = { Text("Exam Seating Plan", fontWeight = FontWeight.SemiBold) },
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
            items.isEmpty() -> EmptyState("No seating plan available", Modifier.padding(padding))
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
            ) { LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items) { s ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EventSeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.courseName ?: s.courseCode ?: "Exam", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                s.courseCode?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                s.examDate?.let { Text("Date: $it", style = MaterialTheme.typography.bodySmall) }
                                s.examTime?.let { Text("Time: $it", style = MaterialTheme.typography.bodySmall) }
                                s.examType?.let { Text("Type: $it", style = MaterialTheme.typography.labelSmall) }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                s.examHall?.let { Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                                s.seatNo?.let { Text("Seat: $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
                                s.row?.let { r -> s.column?.let { c -> Text("R$r-C$c", style = MaterialTheme.typography.labelSmall) } }
                            }
                        }
                    }
                }
            }
            } // PullToRefreshBox
        }
    }
}
