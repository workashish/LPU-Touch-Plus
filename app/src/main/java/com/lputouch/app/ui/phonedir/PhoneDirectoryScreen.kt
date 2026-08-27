package com.lputouch.app.ui.phonedir

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.PhoneContact
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneDirectoryScreen(studentRepository: StudentRepository, onBack: () -> Unit) {
    var items by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    suspend fun load(force: Boolean = false) {
        error = null
        try {
            items = studentRepository.getPhoneDirectory()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load contacts"
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        load()
        loading = false
    }

    val filtered = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else {
            val q = searchQuery.lowercase()
            items.filter {
                it.name?.lowercase()?.contains(q) == true ||
                it.designation?.lowercase()?.contains(q) == true ||
                it.department?.lowercase()?.contains(q) == true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phone Directory", fontWeight = FontWeight.SemiBold) },
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
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search by name, dept...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Clear, null) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                if (filtered.isEmpty()) {
                    EmptyState(if (searchQuery.isBlank()) "No contacts available" else "No results for \"$searchQuery\"")
                } else {
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
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(filtered) { contact ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(contact.name ?: "—", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                            contact.designation?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                            contact.department?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        }
                                        contact.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                                            IconButton(onClick = {
                                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                                            }) {
                                                Icon(Icons.Filled.Phone, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
