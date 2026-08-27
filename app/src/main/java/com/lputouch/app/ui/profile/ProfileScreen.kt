package com.lputouch.app.ui.profile

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lputouch.app.data.api.dto.Address
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
    var refreshing by remember { mutableStateOf(false) }
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
                title = { Text("Profile", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
            )
        },
    ) { padding ->
        val p = profile
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

            else -> PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { scope.launch { refreshing = true; load(); refreshing = false } },
                modifier = Modifier.padding(padding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Hero card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Profile photo (Coil or fallback)
                            val photoUrl = p.studentPicture?.takeIf { it.isNotBlank() && it.startsWith("http") }
                            Box(
                                modifier = Modifier.size(88.dp).clip(CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (photoUrl != null) {
                                    AsyncImage(
                                        model = photoUrl,
                                        contentDescription = "Profile photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(p.studentName ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(p.registrationNumber ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            // Status chip
                            p.studentStatus?.takeIf { it.isNotBlank() }?.let { status ->
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp),
                                ) {
                                    Text(
                                        status,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                StatChip("CGPA", p.cgpa ?: "—")
                                StatChip("Attendance", p.aggAttendance ?: "—")
                                p.currentBalance?.takeIf { it.isNotBlank() }?.let {
                                    StatChip("Balance", it)
                                }
                            }
                        }
                    }

                    InfoCard("Academic", listOf(
                        "Program" to (p.programName ?: "—"),
                        "Session" to (p.admissionSession ?: "—"),
                        "Batch" to (p.batchYear ?: "—"),
                        "Section" to (p.section ?: "—"),
                        "Category" to (p.categoryCode ?: "—"),
                        "Term" to (p.termId?.takeIf { it.isNotBlank() } ?: "—"),
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
                        "Hostel" to (p.hostel?.takeIf { it.isNotBlank() } ?: "—"),
                    ))

                    // Addresses
                    p.correspondingAddress?.let { addr ->
                        AddressCard("Corresponding Address", addr)
                    }
                    p.permanentAddress?.let { addr ->
                        AddressCard("Permanent Address", addr)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}

@Composable
private fun InfoCard(title: String, rows: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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

@Composable
private fun AddressCard(title: String, addr: Address) {
    val lines = listOfNotNull(
        addr.houseNo?.takeIf { it.isNotBlank() },
        addr.colony?.takeIf { it.isNotBlank() },
        addr.cityName?.takeIf { it.isNotBlank() },
        addr.districtName?.takeIf { it.isNotBlank() },
        addr.stateName?.takeIf { it.isNotBlank() },
        addr.pinCode?.takeIf { it.isNotBlank() },
        addr.countryName?.takeIf { it.isNotBlank() },
    )
    if (lines.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                text = lines.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
