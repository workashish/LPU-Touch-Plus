package com.lputouch.app.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.lputouch.app.data.prefs.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Full-screen gate shown before content when biometric or PIN lock is enabled.
 */
@Composable
fun AppLockGate(
    sessionStore: SessionStore,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var biometricEnabled by remember { mutableStateOf(false) }
    var pinEnabled by remember { mutableStateOf(false) }
    var pinHash by remember { mutableStateOf<String?>(null) }

    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var bioAvailable by remember { mutableStateOf(false) }
    var bioError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        biometricEnabled = sessionStore.biometricEnabled.first()
        pinEnabled = sessionStore.pinEnabled.first()
        pinHash = sessionStore.pinHash.first()
        if (biometricEnabled) {
            val manager = BiometricManager.from(context)
            bioAvailable = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
            if (bioAvailable) {
                promptBiometric(context, onSuccess = onUnlocked)
            } else if (!pinEnabled) {
                // No way in — never trap the user: offer to sign out and log in again.
                bioError = true
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text("App locked", style = MaterialTheme.typography.headlineSmall)

        if (pinEnabled) {
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = pinInput,
                onValueChange = { input ->
                    if (input.length <= 4 && input.all { it.isDigit() }) {
                        pinInput = input
                        pinError = false
                    }
                    if (input.length == 4) {
                        if (pinHash != null && PinHelper.verify(input, pinHash!!)) {
                            onUnlocked()
                        } else {
                            pinError = true
                            pinInput = ""
                        }
                    }
                },
                label = { Text("Enter PIN") },
                singleLine = true,
                isError = pinError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
            )
            if (pinError) {
                Spacer(Modifier.height(8.dp))
                Text("Wrong PIN", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                if (pinHash != null && PinHelper.verify(pinInput, pinHash!!)) onUnlocked()
            }) { Text("Unlock") }
        } else if (biometricEnabled) {
            if (bioError) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Biometrics are no longer available on this device. Sign out and log in again to continue.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            sessionStore.logout()
                            // Note: loggedIn flow will emit false, triggering navigation to login.
                            // We do NOT call onUnlocked() here — that would briefly show the main screen.
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Sign out") }
            } else {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { promptBiometric(context, onSuccess = onUnlocked) },
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Unlock with biometrics") }
            }
        }
    }
}

private fun promptBiometric(context: Context, onSuccess: () -> Unit) {
    val activity = context as? FragmentActivity ?: return
    val executor = ContextCompat.getMainExecutor(context)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock LPU Touch Plus")
            .setSubtitle("Use biometrics to continue")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()
    )
}
