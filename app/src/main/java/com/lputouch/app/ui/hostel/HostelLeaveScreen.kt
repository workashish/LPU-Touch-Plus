package com.lputouch.app.ui.hostel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.HostelLeaveBalance
import com.lputouch.app.data.api.dto.HostelLeaveItem
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import com.lputouch.app.util.rememberNetworkAvailability
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostelLeaveScreen(studentRepository: StudentRepository, onBack: () -> Unit) {
    var history by remember { mutableStateOf<List<HostelLeaveItem>>(emptyList()) }
    var balance by remember { mutableStateOf<HostelLeaveBalance?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val isOnline = rememberNetworkAvailability()

    suspend fun load(force: Boolean = false) {
        error = null
        if (!isOnline) { error = "No internet connection"; return }
        try {
            balance = studentRepository.getHostelLeaveBalance()
            history = studentRepository.getHostelLeaveHistory()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load hostel leave data"
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
                title = { Text("Hostel Leave", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
    ) { padding ->
        when {
            loading -> LoadingState(Modifier.padding(padding))
            error != null && history.isEmpty() && balance == null -> ErrorState(
                message = error!!,
                onRetry = { scope.launch { loading = true; load(); loading = false } },
                modifier = Modifier.padding(padding),
            )
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
                balance?.let { bal ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                LeaveStatColumn("Total", bal.totalLeaves ?: "—")
                                LeaveStatColumn("Used", bal.usedLeaves ?: "—")
                                LeaveStatColumn("Balance", bal.balanceLeaves ?: "—")
                            }
                        }
                    }
                }
                if (history.isEmpty()) {
                    item { EmptyState("No leave history") }
                } else {
                    item {
                        Text("Leave History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(history) { leave ->
                        val statusColor = when {
                            leave.status?.contains("approved", ignoreCase = true) == true -> MaterialTheme.colorScheme.primary
                            leave.status?.contains("reject", ignoreCase = true) == true -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row {
                                    Column(Modifier.weight(1f)) {
                                        Text(leave.reason ?: "Leave", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        leave.leaveType?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        leave.fromDate?.let { Text("From: $it", style = MaterialTheme.typography.bodySmall) }
                                        leave.toDate?.let { Text("To: $it", style = MaterialTheme.typography.bodySmall) }
                                    }
                                    leave.status?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.SemiBold) }
                                }
                            }
                        }
                    }
                }
            }
            } // PullToRefreshBox
        }
    }
}

@Composable
private fun LeaveStatColumn(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}
