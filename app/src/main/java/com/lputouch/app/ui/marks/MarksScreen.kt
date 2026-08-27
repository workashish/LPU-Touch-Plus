package com.lputouch.app.ui.marks

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Info
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
import com.lputouch.app.util.rememberNetworkAvailability
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.ResultItem
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import kotlinx.coroutines.launch

/** Groups results into terms, preserving server order (I, II, III...). */
private fun groupByTerm(results: List<ResultItem>): List<Pair<String, List<ResultItem>>> {
    val order = mutableListOf<String>()
    val map = LinkedHashMap<String, MutableList<ResultItem>>()
    for (r in results) {
        val term = r.detailTerm?.takeIf { it.isNotBlank() } ?: r.romanTerm ?: "Results"
        if (!map.containsKey(term)) {
            map[term] = mutableListOf()
            order.add(term)
        }
        map[term]!!.add(r)
    }
    return order.mapNotNull { term -> map[term]?.let { term to it } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarksScreen(
    studentRepository: StudentRepository,
    onBack: () -> Unit,
) {
    var results by remember { mutableStateOf<List<ResultItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val isOnline = rememberNetworkAvailability()

    suspend fun load(force: Boolean = false) {
        error = null
        if (!isOnline) { error = "No internet connection"; return }
        try {
            results = studentRepository.getResults(forceRefresh = force)
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
                title = { Text("Marks & Results") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        when {
            loading -> LoadingState(Modifier.padding(padding))

            error != null && results.isEmpty() -> ErrorState(
                message = error!!,
                onRetry = { scope.launch { loading = true; load(); loading = false } },
                modifier = Modifier.padding(padding),
            )

            results.isEmpty() -> EmptyState("No results found", Modifier.padding(padding))

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
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val terms = groupByTerm(results)
                    items(terms, key = { it.first }) { (term, itemsInTerm) ->
                        TermSection(term = term, items = itemsInTerm)
                    }
                }
            }
        }
    }
}

@Composable
private fun TermSection(term: String, items: List<ResultItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = term,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1f))
            // Term summary: TGPA + CGPA from the first item that has values.
            val tgpa = items.firstNotNullOfOrNull { it.termPercentOrTGPA?.removePrefix("TGPA : ") }
            val cgpa = items.firstNotNullOfOrNull { it.termPercentOrCGPA?.removePrefix("CGPA : ") }
            if (cgpa != null) {
                Text(
                    text = "CGPA $cgpa",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (tgpa != null) {
                Spacer(Modifier.padding(start = 8.dp))
                Text(
                    text = "TGPA $tgpa",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "${items.size} courses",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        items.forEach { r ->
            ResultCard(r)
        }
    }
}

@Composable
private fun ResultCard(r: ResultItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = r.course?.substringAfter(":: ")?.trim() ?: r.course ?: r.courseCode ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                    )
                    r.courseCode?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.padding(start = 8.dp))
                val gradeColor = parseColor(r.gradeColor) ?: MaterialTheme.colorScheme.primary
                Text(
                    text = r.gradeOrMarks?.removePrefix("Grade : ") ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = gradeColor,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                LabeledValue("TGPA", r.termPercentOrTGPA?.removePrefix("TGPA : ") ?: "—")
                LabeledValue("CGPA", r.termPercentOrCGPA?.removePrefix("CGPA : ") ?: "—")
                LabeledValue("Credits", r.mulFactor ?: "—")
            }
            r.gradeExplanation?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = parseColor(r.gradeColor) ?: MaterialTheme.colorScheme.primary,
                )
            }
            r.failGradeDescription?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.padding(start = 4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

private fun parseColor(hex: String?): Color? =
    runCatching { Color(android.graphics.Color.parseColor(hex!!)) }.getOrNull()
