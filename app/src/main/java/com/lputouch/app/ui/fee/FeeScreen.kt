package com.lputouch.app.ui.fee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.api.dto.FeeBalanceItem
import com.lputouch.app.data.api.dto.FeeExtensionItem
import com.lputouch.app.data.repo.StudentRepository
import com.lputouch.app.ui.components.EmptyState
import com.lputouch.app.ui.components.LoadingState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeScreen(studentRepository: StudentRepository, onBack: () -> Unit) {
    var feeBalance by remember { mutableStateOf<List<FeeBalanceItem>>(emptyList()) }
    var feeExtension by remember { mutableStateOf<List<FeeExtensionItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loading = true
        feeBalance = studentRepository.getFeeBalance()
        feeExtension = studentRepository.getFeeExtensionPopup()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fee Balance", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
            )
        }
    ) { padding ->
        if (loading) { LoadingState(Modifier.padding(padding)); return@Scaffold }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Extension popup warning
            feeExtension.firstOrNull()?.let { ext ->
                ext.displayMessage?.takeIf { it.isNotBlank() }?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFF57C00))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Fee Extension Notice", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                Text(msg, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8D3D00))
                                ext.feeExtensionDate?.takeIf { it.isNotBlank() }?.let {
                                    Text("Extended till: $it", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8D3D00))
                                }
                            }
                        }
                    }
                }
            }

            if (feeBalance.isEmpty()) {
                EmptyState("No fee balance information available")
            } else {
                feeBalance.forEach { fee ->
                    FeeCard(fee)
                }
            }
        }
    }
}

@Composable
private fun FeeCard(fee: FeeBalanceItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(fee.feeHead ?: "Fee", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                fee.dueDate?.takeIf { it.isNotBlank() }?.let {
                    Text("Due: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                fee.status?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = if (it.contains("clear", true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                (fee.balance ?: fee.currentBalance ?: fee.amount)?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
