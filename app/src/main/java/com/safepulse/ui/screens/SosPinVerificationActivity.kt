package com.safepulse.ui.screens

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.safepulse.data.security.PinVerificationResult
import com.safepulse.service.SafetyFeatureManager
import com.safepulse.service.SafetyForegroundService
import com.safepulse.ui.theme.SafePulseTheme

class SosPinVerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)

        setContent {
            SafePulseTheme {
                PinVerificationDialog(
                    onDismiss = { finish() },
                    onVerify = { pin ->
                        val result = SafetyForegroundService.getInstance()?.cancelEmergencyWithPin(pin)
                            ?: SafetyFeatureManager.getInstance(applicationContext).handleCancelPin(pin)

                        when (result) {
                            PinVerificationResult.NORMAL_CANCELLED,
                            PinVerificationResult.DURESS_ACTIVATED,
                            PinVerificationResult.DISABLED_CANCELLED -> {
                                Toast.makeText(
                                    this,
                                    "SOS cancelled successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                                true
                            }
                            PinVerificationResult.INVALID -> false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PinVerificationDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN Verification") },
        text = {
            Column {
                Text(
                    "Enter your safety PIN to cancel SOS.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter(Char::isDigit).take(6)
                        error = null
                    },
                    label = { Text("PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!onVerify(pin)) {
                        error = "Invalid PIN"
                    }
                },
                enabled = pin.length in 4..6
            ) {
                Text("Verify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
