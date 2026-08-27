package com.lputouch.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lputouch.app.data.prefs.SessionStore
import com.lputouch.app.security.PinHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    sessionStore: SessionStore,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val darkMode by sessionStore.darkMode.collectAsState(initial = false)
    val biometricEnabled by sessionStore.biometricEnabled.collectAsState(initial = false)
    val pinEnabled by sessionStore.pinEnabled.collectAsState(initial = false)

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingCard(title = "Appearance") {
                SettingRow(
                    label = "Dark mode",
                    subtitle = "Use the dark theme",
                ) {
                    Switch(
                        checked = darkMode,
                        onCheckedChange = { scope.launch { sessionStore.setDarkMode(it) } },
                    )
                }
            }

            SettingCard(title = "Security") {
                SettingRow(
                    label = "Biometric unlock",
                    subtitle = "Unlock the app with fingerprint / face",
                ) {
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { scope.launch { sessionStore.setBiometricEnabled(it) } },
                    )
                }
                SettingRow(
                    label = "PIN lock",
                    subtitle = "Require a 4-digit PIN to open the app",
                ) {
                    Switch(
                        checked = pinEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                pinInput = ""
                                showPinDialog = true
                            } else {
                                scope.launch { sessionStore.setPin(false) }
                            }
                        },
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("About", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "LPU Touch Plus v1.0.0 — an unofficial student app for Lovely Professional University. " +
                                "Not affiliated with LPU. Use only your own credentials.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set a 4-digit PIN") },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) pinInput = input
                    },
                    singleLine = true,
                    label = { Text("PIN") },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = pinInput.length == 4,
                    onClick = {
                        scope.launch {
                            sessionStore.setPin(true, PinHelper.hash(pinInput))
                            showPinDialog = false
                        }
                    },
                ) { Text("Enable") }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}
