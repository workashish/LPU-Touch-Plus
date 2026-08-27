package com.lputouch.app.ui.placement

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.PlacementDrive
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacementScreen(
    studentRepository: StudentRepository,
    onBack: () -> Unit,
) {
    var drives by remember { mutableStateOf<List<PlacementDrive>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    suspend fun load(force: Boolean = false) {
        error = null
        try {
            drives = studentRepository.getPlacementDrives()
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
                title = { Text("Placement Drives") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        when {
            loading -> LoadingState(Modifier.padding(padding))
            error != null && drives.isEmpty() -> ErrorState(error!!, onRetry = { scope.launch { loading = true; load(); loading = false } }, modifier = Modifier.padding(padding))
            drives.isEmpty() -> EmptyState("No drives available", Modifier.padding(padding))
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(drives, key = { it.id ?: it.driveCode ?: it.hashCode().toString() }) { d ->
                    DriveCard(d, onOpenLink = {
                        d.companyLink?.takeIf { it.startsWith("http") }?.let { url ->
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        }
                    })
                }
            }
            } // PullToRefreshBox
        }
    }
}

@Composable
private fun DriveCard(d: PlacementDrive, onOpenLink: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Work, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(start = 8.dp))
                Text(d.companyName ?: "", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            d.designation?.takeIf { it.isNotBlank() && it != "NotifiedLater" }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            InfoRow("Drive", d.driveCode)
            InfoRow("Eligibility", d.isEligibleText)
            InfoRow("Package", d.salaryPackage)
            InfoRow("Venue", d.venue)
            InfoRow("Last date", d.lastDate)
            if (d.companyLink?.startsWith("http") == true) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = onOpenLink, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.padding(start = 6.dp))
                    Text("Open company link")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Spacer(Modifier.height(4.dp))
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.65f),
        )
    }
}
