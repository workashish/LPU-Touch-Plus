package com.lputouch.app.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.LeaderboardEntry
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(studentRepository: StudentRepository, onBack: () -> Unit) {
    var items by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load(force: Boolean = false) {
        error = null
        try {
            items = studentRepository.getLeaderboard()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load leaderboard"
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
                title = { Text("Leaderboard", fontWeight = FontWeight.SemiBold) },
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
            items.isEmpty() -> EmptyState("No leaderboard data available", Modifier.padding(padding))
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(items) { index, entry ->
                    val rank = entry.rank?.toIntOrNull() ?: (index + 1)
                    val rankColor = when (rank) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFC0C0C0)
                        3 -> Color(0xFFCD7F32)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).background(rankColor.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (rank <= 3) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = rankColor, modifier = Modifier.size(22.dp))
                                } else {
                                    Text("#$rank", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = rankColor)
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.studentName ?: "—", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                entry.registrationNo?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                entry.section?.let { Text("Section: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            entry.score?.let {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("pts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
