package com.safepulse.service

import android.content.Context
import com.safepulse.data.security.PinVerificationResult
import com.safepulse.data.security.SafetyPinRepository
import com.safepulse.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryDeadModeState(
    val isEnabled: Boolean = false,
    val activatedAt: Long = 0L,
    val activationReason: String = "",
    val monitoringActive: Boolean = false,
    val guardianActive: Boolean = false
)

class BatteryDeadModeManager private constructor(private val context: Context) {
    private val appContext = context.applicationContext
    private val safetyFeatureManager = SafetyFeatureManager.getInstance(appContext)
    private val safetyPinRepository = SafetyPinRepository.getInstance(appContext)
    @Suppress("unused")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(BatteryDeadModeState())
    val state: StateFlow<BatteryDeadModeState> = _state.asStateFlow()

    fun activate(reason: String = "User enabled from Advanced Safety") {
        if (_state.value.isEnabled) return

        SafetyForegroundService.start(appContext)
        _state.value = BatteryDeadModeState(
            isEnabled = true,
            activatedAt = System.currentTimeMillis(),
            activationReason = reason,
            monitoringActive = true,
            guardianActive = true
        )

        safetyFeatureManager.appendTimeline(
            title = "Battery dead mode enabled",
            detail = reason,
            location = null,
            eventType = "BATTERY_DEAD_MODE_ENABLED"
        )
        NotificationHelper.showDefaultForegroundNotification(appContext)
    }

    fun deactivate(reason: String = "Secure exit verified") {
        if (!_state.value.isEnabled) return

        _state.value = BatteryDeadModeState()
        safetyFeatureManager.appendTimeline(
            title = "Battery dead mode disabled",
            detail = reason,
            location = null,
            eventType = "BATTERY_DEAD_MODE_DISABLED"
        )
        NotificationHelper.showDefaultForegroundNotification(appContext)
    }

    fun verifyExitPin(pin: String): Boolean {
        return when (safetyPinRepository.verifyPin(pin)) {
            PinVerificationResult.NORMAL_CANCELLED,
            PinVerificationResult.DISABLED_CANCELLED -> {
                deactivate("Normal PIN verified")
                true
            }
            PinVerificationResult.DURESS_ACTIVATED -> {
                safetyFeatureManager.handleCancelPin(pin)
                false
            }
            PinVerificationResult.INVALID -> false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: BatteryDeadModeManager? = null

        fun getInstance(context: Context): BatteryDeadModeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BatteryDeadModeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
