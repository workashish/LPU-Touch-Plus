package com.lputouch.app.ui.announcements

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Campaign
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.AnnouncementDetail
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementDetailScreen(
    announcementId: String,
    tab: String,
    studentRepository: StudentRepository,
    onBack: () -> Unit,
) {
    var detail by remember { mutableStateOf<AnnouncementDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    suspend fun load() {
        detail = studentRepository.getAnnouncementDetail(announcementId, tab)
        if (detail == null) error = "Could not load announcement"
    }

    LaunchedEffect(Unit) {
        loading = true
        load()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Announcement") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        when {
            loading -> LoadingState(Modifier.padding(padding))
            error != null -> ErrorState(message = error!!, onRetry = {
                scope.launch {
                    loading = true
                    error = null
                    load()
                    loading = false
                }
            }, modifier = Modifier.padding(padding))
            detail == null -> ErrorState(message = "Could not load announcement", onRetry = {}, modifier = Modifier.padding(padding))
            else -> {
                val d = detail!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = d.subject ?: "",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    d.entryDate?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    d.uploadedBy?.takeIf { it.isNotBlank() }?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Campaign,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.padding(start = 4.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    val body = d.description?.takeIf { it.isNotBlank() }
                    if (body != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(
                                text = stripHtml(body),
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    val files = d.file?.filter { !it.filePath.isNullOrBlank() }
                    if (!files.isNullOrEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Attachments", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                files.forEach { f ->
                                    TextButton(onClick = {
                                        f.filePath?.let { url ->
                                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                        }
                                    }) {
                                        Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.padding(start = 6.dp))
                                        Text(
                                            text = f.fileName ?: f.filePath ?: "Open attachment",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
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

/** Minimal HTML-to-text for the announcement body (LPU sends rich HTML). */
private fun stripHtml(html: String): String {
    var s = html
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</li>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), "")
    // Decode common HTML entities (named + numeric).
    s = s.replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&mdash;", "—")
        .replace("&ndash;", "–")
        .replace("&rsquo;", "'")
        .replace("&lsquo;", "'")
        .replace("&ldquo;", "\"")
        .replace("&rdquo;", "\"")
    // Numeric entities: &#10; (newline), &#13; (CR), &#39; (apostrophe) etc.
    s = Regex("&#(\\d+);").replace(s) { m ->
        val code = m.groupValues[1].toIntOrNull() ?: return@replace m.value
        when (code) {
            10, 13 -> "\n"
            39 -> "'"
            34 -> "\""
            160 -> " "
            else -> code.toChar().toString()
        }
    }
    return s
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}
