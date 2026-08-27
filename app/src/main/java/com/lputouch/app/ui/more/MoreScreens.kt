package com.lputouch.app.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.AptitudeScore
import com.lputouch.app.data.api.dto.MakeupClass
import com.lputouch.app.data.api.dto.MentorRemark
import com.lputouch.app.data.api.dto.RmsQuery
import com.lputouch.app.data.api.dto.RplResult
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.SimpleListScreen
import kotlinx.coroutines.launch

/* ------------------------------- Mentor Remarks ---------------------------- */

@Composable
fun MentorRemarksScreen(
    studentRepository: StudentRepository,
    onBack: () -> Unit,
) {
    var items by remember { mutableStateOf<List<MentorRemark>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        error = null
        try { items = studentRepository.getMentorRemarks() } catch (e: Exception) { error = e.message ?: "Failed to load" }
    }

    LaunchedEffect(Unit) { loading = true; load(); loading = false }

    SimpleListScreen(
        title = "Mentor Remarks",
        loading = loading,
        error = error,
        items = items,
        emptyMessage = "No mentor remarks found",
        onBack = onBack,
        onRetry = { scope.launch { loading = true; load(); loading = false } },
        itemKey = { "${it.meetingDate}-${it.hashCode()}" },
    ) { r ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.meetingDate ?: "", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(
                        text = when (r.attendanceStatus?.uppercase()) {
                            "P" -> "Present"
                            "A" -> "Absent"
                            else -> r.attendanceStatus ?: ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                r.mentor?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                r.remarks?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/* ------------------------------- RMS Queries ------------------------------- */

@Composable
fun RmsQueriesScreen(
    studentRepository: StudentRepository,
    onBack: () -> Unit,
) {
    var items by remember { mutableStateOf<List<RmsQuery>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        error = null
        try { items = studentRepository.getRmsQueries() } catch (e: Exception) { error = e.message ?: "Failed to load" }
    }

    LaunchedEffect(Unit) { loading = true; load(); loading = false }

    SimpleListScreen(
        title = "My Queries",
        loading = loading,
        error = error,
        items = items,
        emptyMessage = "No queries found",
        onBack = onBack,
        onRetry = { scope.launch { loading = true; load(); loading = false } },
        itemKey = { it.id ?: it.hashCode().toString() },
    ) { q ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(q.subject ?: "Query", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(
                        text = q.status ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (q.status?.equals("Close", true) == true) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                    )
                }
                q.ticketNo?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                q.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                }
                q.remarks?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                }
                q.originDate?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/* ---------------------------------- Scores -------------------------------- */

@Composable
fun AptitudeScoresScreen(
    studentRepository: StudentRepository,
    onBack: () -> Unit,
) {
    var amcat by remember { mutableStateOf<List<AptitudeScore>>(emptyList()) }
    var cocubes by remember { mutableStateOf<List<AptitudeScore>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        error = null
        try {
            amcat = studentRepository.getAmcatScore()
            cocubes = studentRepository.getCoCubesScore()
        } catch (e: Exception) { error = e.message ?: "Failed to load" }
    }

    LaunchedEffect(Unit) { loading = true; load(); loading = false }

    SimpleListScreen(
        title = "Aptitude Scores",
        loading = loading,
        error = error,
        items = listOf("AMCAT" to amcat, "CoCubes" to cocubes).filter { it.second.isNotEmpty() },
        emptyMessage = "No aptitude scores found",
        onBack = onBack,
        onRetry = { scope.launch { loading = true; load(); loading = false } },
        itemKey = { it.first },
    ) { (suiteName, scores) ->
        Column {
            Text(suiteName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            scores.forEach { s ->
                ScoreCard(s)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ScoreCard(s: AptitudeScore) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(start = 6.dp))
                Text(s.testName ?: "", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(10.dp))
            ScoreRow("English", s.english)
            ScoreRow("Quantitative", s.quantitativeAbility)
            ScoreRow("Logical", s.logicalAbility)
        }
    }
}

@Composable
private fun ScoreRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Spacer(Modifier.height(4.dp))
    Row {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

/* ----------------------------------- RPL ---------------------------------- */

@Composable
fun RplScreen(
    studentRepository: StudentRepository,
    onBack: () -> Unit,
) {
    var items by remember { mutableStateOf<List<RplResult>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        error = null
        try { items = studentRepository.getRplResults() } catch (e: Exception) { error = e.message ?: "Failed to load" }
    }

    LaunchedEffect(Unit) { loading = true; load(); loading = false }

    SimpleListScreen(
        title = "RPL Results",
        loading = loading,
        error = error,
        items = items,
        emptyMessage = "No RPL results found",
        onBack = onBack,
        onRetry = { scope.launch { loading = true; load(); loading = false } },
        itemKey = { "${it.courseCode}-${it.termId}" },
    ) { r ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.courseName ?: r.courseCode ?: "", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text(r.grade ?: "", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                r.courseCode?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                r.rplDesc?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/* ------------------------------ Makeup Classes ----------------------------- */

@Composable
fun MakeupScreen(
    studentRepository: StudentRepository,
    onBack: () -> Unit,
) {
    var items by remember { mutableStateOf<List<MakeupClass>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        error = null
        try { items = studentRepository.getMakeupClasses() } catch (e: Exception) { error = e.message ?: "Failed to load" }
    }

    LaunchedEffect(Unit) { loading = true; load(); loading = false }

    SimpleListScreen(
        title = "Makeup & Adjustment",
        loading = loading,
        error = error,
        items = items,
        emptyMessage = "No makeup classes scheduled",
        onBack = onBack,
        onRetry = { scope.launch { loading = true; load(); loading = false } },
        itemKey = { "${it.subjectName}-${it.attendanceDate}-${it.hashCode()}" },
    ) { m ->
        // Determine display values — handle both new and legacy API formats
        val courseName = m.courseCode?.takeIf { it.isNotBlank() }
            ?: m.subjectName?.takeIf { it.isNotBlank() }
            ?: m.courseName?.takeIf { it.isNotBlank() }
            ?: ""
        val time = m.lectureTime?.takeIf { it.isNotBlank() }
            ?: m.attendanceTime?.takeIf { it.isNotBlank() }
            ?: ""
        val faculty = m.makeupBy?.takeIf { it.isNotBlank() }
            ?.substringBefore(":")?.trim()
            ?: m.facultyName?.takeIf { it.isNotBlank() }
            ?: ""
        val date = m.makeupDate?.takeIf { it.isNotBlank() }
            ?: m.attendanceDate?.takeIf { it.isNotBlank() }
            ?: ""
        val category = m.category?.takeIf { it.isNotBlank() }
            ?: m.type?.takeIf { it.isNotBlank() }
            ?: ""
        val section = m.sectionNo?.takeIf { it.isNotBlank() }
            ?: m.day?.takeIf { it.isNotBlank() }
            ?: ""
        val attType = m.attendanceType?.takeIf { it.isNotBlank() } ?: ""
        val description = m.description?.takeIf { it.isNotBlank() } ?: ""

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                // Row 1: Course name + Category badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        courseName,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                    )
                    category.takeIf { it.isNotBlank() }?.let { t ->
                        Spacer(Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (t.contains("makeup", true))
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                t,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Description
                description.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Section + Type tags
                val tags = listOfNotNull(
                    section.takeIf { it.isNotBlank() }?.let { "Sec: $it" },
                    attType.takeIf { it.isNotBlank() },
                    m.groupNo?.takeIf { it.isNotBlank() }?.let { "Grp: $it" },
                )
                if (tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { tag ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    tag,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }

                // Details grid: Date, Time, Room, Faculty
                val details = listOfNotNull(
                    date.takeIf { it.isNotBlank() }?.let { "Date" to it },
                    time.takeIf { it.isNotBlank() }?.let { "Time" to it },
                    m.roomNo?.takeIf { it.isNotBlank() }?.let { "Room" to it },
                    faculty.takeIf { it.isNotBlank() }?.let { "Faculty" to it },
                )
                if (details.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    details.forEach { (k, v) ->
                        Spacer(Modifier.height(4.dp))
                        Row {
                            Text(k, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.3f))
                            Text(v, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.7f))
                        }
                    }
                }
            }
        }
    }
}
