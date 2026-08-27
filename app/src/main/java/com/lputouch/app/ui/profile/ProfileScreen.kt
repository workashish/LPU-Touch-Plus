package com.lputouch.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.lputouch.app.data.api.dto.StudentBasicInfo
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    studentRepository: StudentRepository,
    onBack: () -> Unit,
) {
    var profile by remember { mutableStateOf<StudentBasicInfo?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        error = null
        try {
            profile = studentRepository.getProfile()
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
                title = { Text("Profile") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        val p = profile
        // Never render an expired/empty server placeholder as if it were real data.
        val usable = p?.takeIf { it.error.isNullOrBlank() && !it.studentName.isNullOrBlank() }
        when {
            loading -> LoadingState(Modifier.padding(padding))

            error != null && usable == null -> ErrorState(
                message = error!!,
                onRetry = { scope.launch { loading = true; load(); loading = false } },
                modifier = Modifier.padding(padding),
            )

            usable == null -> ErrorState(
                message = "Could not load profile",
                onRetry = { scope.launch { loading = true; load(); loading = false } },
                modifier = Modifier.padding(padding),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(p.studentName ?: "—", style = MaterialTheme.typography.titleLarge)
                        Text(p.registrationNumber ?: "", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatChip("CGPA", p.cgpa ?: "—")
                            StatChip("Attendance", p.aggAttendance ?: "—")
                        }
                    }
                }

                InfoCard("Academic", listOf(
                    "Program" to (p.programName ?: "—"),
                    "Session" to (p.admissionSession ?: "—"),
                    "Batch" to (p.batchYear ?: "—"),
                    "Section" to (p.section ?: "—"),
                    "Category" to (p.categoryCode ?: "—"),
                ))
                InfoCard("Contact", listOf(
                    "Email" to (p.studentEmail ?: "—"),
                    "Mobile" to (p.studentMobile ?: "—"),
                ))
                InfoCard("Personal", listOf(
                    "Father" to (p.fatherName ?: "—"),
                    "Mother" to (p.motherName ?: "—"),
                    "DOB" to (p.dateOfBirth ?: "—"),
                    "Gender" to (p.gender ?: "—"),
                    "Nationality" to (p.nationality ?: "—"),
                ))
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}

@Composable
private fun InfoCard(title: String, rows: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            rows.forEachIndexed { i, (k, v) ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                Row {
                    Text(
                        text = k,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.4f),
                    )
                    Text(
                        text = v,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(0.6f),
                    )
                }
            }
        }
    }
}
