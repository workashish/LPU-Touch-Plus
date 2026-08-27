package com.lputouch.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.LibraryItem
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(studentRepository: StudentRepository, onBack: () -> Unit) {
    var items by remember { mutableStateOf<List<LibraryItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load(force: Boolean = false) {
        error = null
        try {
            items = studentRepository.getLibraryData()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load library data"
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
                title = { Text("Library", fontWeight = FontWeight.SemiBold) },
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
            items.isEmpty() -> EmptyState("No books currently issued", Modifier.padding(padding))
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
                items(items) { book ->
                    val hasFine = (book.fine?.toDoubleOrNull() ?: 0.0) > 0
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(book.bookName ?: "Book", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                book.author?.takeIf { it.isNotBlank() }?.let { Text("by $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                Spacer(Modifier.height(8.dp))
                                book.issueDate?.let { Text("Issued: $it", style = MaterialTheme.typography.labelSmall) }
                                book.dueDate?.let { Text("Due: $it", style = MaterialTheme.typography.labelSmall, color = if (hasFine) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface) }
                                if (hasFine) {
                                    Spacer(Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Fine: ₹${book.fine}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
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
