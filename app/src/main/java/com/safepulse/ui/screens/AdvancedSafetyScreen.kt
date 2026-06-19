package com.safepulse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safepulse.data.security.PinVerificationResult
import com.safepulse.service.BatteryDeadModeManager
import com.safepulse.service.BatteryDeadModeState
import com.safepulse.ui.theme.PrimaryRed
import com.safepulse.ui.theme.SafeGreen
import com.safepulse.ui.theme.WarningYellow
import com.safepulse.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSafetyScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val batteryDeadModeManager = remember(context) { BatteryDeadModeManager.getInstance(context) }
    val batteryDeadState by batteryDeadModeManager.state.collectAsState()
    var showBatteryDeadConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = PrimaryRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Advanced Safety",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            DuressCancelPinCard(
                onCancelPin = { viewModel.handleCancelPin(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            BatteryDeadModeCard(
                state = batteryDeadState,
                onEnable = { showBatteryDeadConfirm = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showBatteryDeadConfirm) {
        AlertDialog(
            onDismissRequest = { showBatteryDeadConfirm = false },
            title = { Text("Enable Battery Dead Mode?") },
            text = {
                Text("Your phone will appear inactive while safety monitoring continues.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatteryDeadConfirm = false
                        batteryDeadModeManager.activate("Enabled from Advanced Safety")
                    }
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryDeadConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BatteryDeadModeCard(
    state: BatteryDeadModeState,
    onEnable: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Battery0Bar,
                    contentDescription = null,
                    tint = if (state.isEnabled) SafeGreen else WarningYellow
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Fake Battery Dead Mode", fontWeight = FontWeight.Medium)
                    Text(
                        if (state.isEnabled) {
                            "Status: Enabled"
                        } else {
                            "Make the phone appear inactive while protection continues."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (state.isEnabled) {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Overlay Active")
                }
            } else {
                Button(
                    onClick = onEnable,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enable Battery Dead Mode")
                }
            }
        }
    }
}

@Composable
private fun DuressCancelPinCard(
    onCancelPin: (String) -> PinVerificationResult,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }
    var pinResult by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Duress Cancel PIN", fontWeight = FontWeight.Medium)
            Text(
                "Configured in Settings. Normal PIN cancels SOS; duress PIN keeps protection active silently.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.take(6) },
                    label = { Text("PIN") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        pinResult = when (onCancelPin(pin)) {
                            PinVerificationResult.NORMAL_CANCELLED,
                            PinVerificationResult.DURESS_ACTIVATED,
                            PinVerificationResult.DISABLED_CANCELLED -> "SOS cancelled successfully"
                            PinVerificationResult.INVALID -> "Invalid PIN"
                        }
                        pin = ""
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply")
                }
            }

            pinResult?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.contains("successfully")) SafeGreen else WarningYellow
                )
            }
        }
    }
}
