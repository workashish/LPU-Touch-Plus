package com.lputouch.app.ui.bus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.BusRoute
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusRoutesScreen(studentRepository: StudentRepository, onBack: () -> Unit) {
    var items by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load(force: Boolean = false) {
        error = null
        try {
            items = studentRepository.getBusRoutes()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load bus routes"
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
                title = { Text("Bus Routes", fontWeight = FontWeight.SemiBold) },
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
            items.isEmpty() -> EmptyState("No bus routes available", Modifier.padding(padding))
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
                items(items) { route ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.DirectionsBus, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(route.routeName ?: route.routeNo ?: "Route", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    route.timing?.takeIf { it.isNotBlank() }?.let { Text("Timing: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                                route.routeNo?.let {
                                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
                                        Text(it, modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            route.stops?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(8.dp))
                                Text("Stops: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            route.driver?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Driver: $it", style = MaterialTheme.typography.labelSmall)
                                    route.contact?.let { c ->
                                        Spacer(Modifier.width(8.dp))
                                        Text("($c)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
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
