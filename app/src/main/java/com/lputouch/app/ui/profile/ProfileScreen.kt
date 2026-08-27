package com.lputouch.app.ui.profile

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.lputouch.app.data.api.dto.StudentBasicInfo
import com.lputouch.app.data.api.dto.ProfileSection
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    studentRepository: StudentRepository,
    onBack: () -> Unit,
) {
    var profileSections by remember { mutableStateOf<List<ProfileSection>>(emptyList()) }
    var basicInfo by remember { mutableStateOf<StudentBasicInfo?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        error = null
        try {
            // Fetch sequentially to prevent SessionStore race conditions during token reads
            basicInfo = studentRepository.getStudentBasicInfo()
            profileSections = studentRepository.getProfile()
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
        val sections = profileSections
        val info = basicInfo
        val usable = sections.isNotEmpty() || info != null
        when {
            loading -> LoadingState(Modifier.padding(padding))

            error != null && !usable -> ErrorState(
                message = error!!,
                onRetry = { scope.launch { loading = true; load(); loading = false } },
                modifier = Modifier.padding(padding),
            )

            !usable -> ErrorState(
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
                    val basicSection = sections.find { it.header.equals("Basic", ignoreCase = true) }
                    val name = info?.studentName ?: basicSection?.values?.find { it.title.equals("Name", ignoreCase = true) }?.value ?: "—"
                    val regNo = info?.registrationNumber ?: basicSection?.values?.find { it.title.equals("Registration No.", ignoreCase = true) }?.value ?: ""

                    // Hero card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(16.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        com.lputouch.app.ui.theme.GradientStart,
                                        com.lputouch.app.ui.theme.GradientEnd
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .border(4.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                val rawUrl = info?.studentPicture?.takeIf { it.isNotBlank() } ?: info?.picture
                                val picUrl = resolveProfilePicUrl(rawUrl)
                                if (picUrl == null) {
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
                                } else {
                                    AsyncImage(
                                        model = picUrl,
                                        contentDescription = "Profile Picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(regNo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Core Info (from BasicInfo)
                    if (info != null) {
                        val coreRows = listOfNotNull(
                            info.programName?.let { "Program" to it },
                            info.section?.let { "Section" to it },
                            info.studentEmail?.let { "Email" to it },
                            info.studentMobile?.let { "Mobile" to it },
                            info.fatherName?.let { "Father" to it },
                            info.motherName?.let { "Mother" to it },
                            info.hostel?.takeIf { it.isNotBlank() }?.let { "Hostel" to it }
                        )
                        if (coreRows.isNotEmpty()) {
                            InfoCard(title = "Primary Details", rows = coreRows)
                        }
                    }

                    // Dynamic Info Cards
                    sections.filterNot { it.header.equals("Basic", ignoreCase = true) }.forEach { section ->
                        InfoCard(
                            title = section.header ?: "Details",
                            rows = section.values?.map { Pair(it.title ?: "", it.value ?: "") } ?: emptyList()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Resolves a profile picture string to a Coil-loadable URL.
 * The server sends either:
 * - A path like "/Content/Images/photo.jpg" → prepend https://ums.lpu.in
 * - Raw base64 data like "/9j/4AAQ..." (JPEG) or "iVBORw..." (PNG) → data URI
 */
private fun resolveProfilePicUrl(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()
    // Base64 JPEG starts with /9j/ or iVBOR (PNG) — but paths also start with /
    // Distinguish: base64 strings are very long and contain only base64 chars after the prefix.
    val isBase64 = (trimmed.startsWith("/9j/") || trimmed.startsWith("iVBOR") ||
            trimmed.startsWith("data:image")) && trimmed.length > 500
    return when {
        trimmed.startsWith("data:") -> trimmed
        isBase64 -> {
            val mimeType = when {
                trimmed.startsWith("/9j/") -> "image/jpeg"
                trimmed.startsWith("iVBOR") -> "image/png"
                else -> "image/jpeg"
            }
            "data:$mimeType;base64,$trimmed"
        }
        trimmed.startsWith("/") -> "https://ums.lpu.in$trimmed"
        trimmed.startsWith("http") -> trimmed
        else -> null
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
