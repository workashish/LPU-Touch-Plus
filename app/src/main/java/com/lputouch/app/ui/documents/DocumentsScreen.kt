package com.lputouch.app.ui.documents

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lputouch.app.data.api.dto.AdmissionDocument
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.ErrorState
import com.lputouch.app.ui.components.LoadingState
import com.lputouch.app.util.rememberNetworkAvailability
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(studentRepository: StudentRepository, onBack: () -> Unit) {
    var items by remember { mutableStateOf<List<AdmissionDocument>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedDoc by remember { mutableStateOf<AdmissionDocument?>(null) }
    val scope = rememberCoroutineScope()
    val isOnline = rememberNetworkAvailability()

    suspend fun load(force: Boolean = false) {
        error = null
        if (!isOnline) { error = "No internet connection"; return }
        try {
            items = studentRepository.getAdmissionDocuments()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load documents"
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
                title = { Text("Admission Documents", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
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
            items.isEmpty() -> EmptyState("No documents found", Modifier.padding(padding))
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items, key = { it.documentDescription ?: it.hashCode().toString() }) { doc ->
                        DocumentCard(doc, onClick = { selectedDoc = doc })
                    }
                }
            }
        }
    }

    selectedDoc?.let { doc ->
        DocumentViewerDialog(doc = doc, onDismiss = { selectedDoc = null })
    }
}

@Composable
private fun DocumentCard(doc: AdmissionDocument, onClick: () -> Unit) {
    val status = doc.approvalStatus ?: "Unknown"
    val statusColor = when {
        status.contains("approved", ignoreCase = true) -> Color(0xFF2E7D32)
        status.contains("pending", ignoreCase = true) -> Color(0xFFF57C00)
        status.contains("reject", ignoreCase = true) -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusIcon = when {
        status.contains("approved", ignoreCase = true) -> Icons.Filled.CheckCircle
        status.contains("pending", ignoreCase = true) -> Icons.Filled.HourglassEmpty
        status.contains("reject", ignoreCase = true) -> Icons.Filled.Close
        else -> Icons.Filled.Info
    }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(doc.documentDescription ?: "Document", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(status, style = MaterialTheme.typography.labelSmall, color = statusColor)
                }
            }
            if (!doc.fileBase64.isNullOrBlank()) {
                IconButton(onClick = {
                    scope.launch {
                        saveDocument(context, doc)
                    }
                }) {
                    Icon(Icons.Filled.Download, contentDescription = "Download Document", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun DocumentViewerDialog(doc: AdmissionDocument, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        doc.documentDescription ?: "Document",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (!doc.fileBase64.isNullOrBlank()) {
                        IconButton(onClick = {
                            scope.launch {
                                saveDocument(context, doc)
                            }
                        }) {
                            Icon(Icons.Filled.Download, contentDescription = "Download")
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider()

                val base64 = doc.fileBase64
                if (base64 != null && base64.isNotBlank()) {
                    val isPdf = base64.contains("application/pdf", ignoreCase = true)
                    
                    if (isPdf) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(16.dp))
                            Text("PDF Document", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text("This document is a PDF. Please download it to view it.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = { scope.launch { saveDocument(context, doc) } }) {
                                Icon(Icons.Filled.Download, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Download PDF")
                            }
                        }
                    } else {
                        val bitmap = remember(base64) {
                            try {
                                val clean = base64.substringAfter("base64,")
                                val bytes = Base64.decode(clean, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) { null }
                        }
                        if (bitmap != null) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = doc.documentDescription,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        } else {
                            EmptyState("Unable to display document. Try downloading it.")
                        }
                    }
                } else {
                    EmptyState("No document preview available")
                }
            }
        }
    }
}

private fun saveDocument(context: Context, doc: AdmissionDocument) {
    try {
        val base64 = doc.fileBase64 ?: return

        // Extract mime type and construct extension
        var mimeType = "image/png"
        var ext = "png"
        if (base64.startsWith("data:")) {
            val typePart = base64.substringAfter("data:").substringBefore(";")
            if (typePart.isNotBlank()) {
                mimeType = typePart
                ext = typePart.substringAfterLast("/")
                if (ext == "jpeg") ext = "jpg"
            }
        }

        val clean = base64.substringAfter("base64,")
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        val safeName = doc.documentDescription?.replace(Regex("[^a-zA-Z0-9.-]"), "_") ?: "document"
        val fileName = "${safeName}_${System.currentTimeMillis()}.$ext"

        var savedPath: String? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: Use MediaStore (no permission needed)
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                savedPath = "Downloads/$fileName"
            }
        } else {
            // Android 9 and below: Try external storage, fall back to app cache
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { it.write(bytes) }
                savedPath = file.absolutePath
            } catch (secE: SecurityException) {
                // No WRITE_EXTERNAL_STORAGE permission — save to app cache instead
                val cacheFile = File(context.cacheDir, fileName)
                FileOutputStream(cacheFile).use { it.write(bytes) }
                savedPath = cacheFile.absolutePath
            }
        }

        if (savedPath != null) {
            Toast.makeText(context, "Saved to $savedPath", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Failed to save document", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save document", Toast.LENGTH_SHORT).show()
    }
}
