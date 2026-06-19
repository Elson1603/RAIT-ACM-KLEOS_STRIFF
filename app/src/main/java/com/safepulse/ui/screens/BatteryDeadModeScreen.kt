package com.safepulse.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun BatteryDeadModeScreen(
    onExitPinEntered: (String) -> Boolean
) {
    val view = LocalView.current
    var showExitDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val previousBrightness = window?.attributes?.screenBrightness
        val hadSecureFlag = window?.attributes?.flags
            ?.and(WindowManager.LayoutParams.FLAG_SECURE) != 0

        window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        window?.attributes = window?.attributes?.apply {
            screenBrightness = 0.01f
        }

        onDispose {
            if (window != null) {
                if (!hadSecureFlag) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                window.attributes = window.attributes.apply {
                    screenBrightness = previousBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FakeBatteryIcon()
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                "0%",
                color = Color(0xFF2A2A2A),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Light
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Power Off",
                color = Color(0xFF1A1A1A),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(88.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val releasedBeforeFiveSeconds = withTimeoutOrNull(5_000L) {
                            waitForUpOrCancellation()
                        }
                        if (releasedBeforeFiveSeconds == null) {
                            showExitDialog = true
                        }
                    }
                }
        )
    }

    if (showExitDialog) {
        BatteryDeadExitPinDialog(
            onDismiss = { showExitDialog = false },
            onVerify = { pin ->
                val verified = onExitPinEntered(pin)
                if (verified) {
                    showExitDialog = false
                }
                verified
            }
        )
    }
}

@Composable
private fun FakeBatteryIcon() {
    Box(
        modifier = Modifier
            .width(86.dp)
            .height(38.dp)
            .border(2.dp, Color(0xFF202020), RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 13.dp)
                .width(8.dp)
                .height(18.dp)
                .background(Color(0xFF202020), RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun BatteryDeadExitPinDialog(
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
