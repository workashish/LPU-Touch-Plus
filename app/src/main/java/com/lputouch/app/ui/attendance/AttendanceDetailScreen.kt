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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.AttendanceDetailItem
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDetailScreen(
    studentRepository: StudentRepository,
    courseCode: String,
    onBack: () -> Unit,
) {
    var items by remember { mutableStateOf<List<AttendanceDetailItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(courseCode) {
        loading = true
        items = studentRepository.getAttendanceDetail(courseCode)
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
            items.isEmpty() -> EmptyState("No attendance detail available", Modifier.padding(padding))
            else -> {
                val present = items.count { it.status?.equals("P", ignoreCase = true) == true }
                val absent = items.size - present

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
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
                            }
                        }
                    }

                    items(items) { att ->
                        val isPresent = att.status?.equals("P", ignoreCase = true) == true
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(12.dp).background(
                                        if (isPresent) Color(0xFF2E7D32) else Color(0xFFC62828),
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
                                    text = if (isPresent) "Present" else "Absent",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isPresent) Color(0xFF2E7D32) else Color(0xFFC62828),
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
