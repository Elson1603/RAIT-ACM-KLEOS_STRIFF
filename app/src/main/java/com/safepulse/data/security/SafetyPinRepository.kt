package com.safepulse.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

data class SafetyPinConfig(
    val normalPin: String,
    val duressPin: String,
    val isEnabled: Boolean
)

enum class PinVerificationResult {
    NORMAL_CANCELLED,
    DURESS_ACTIVATED,
    INVALID,
    DISABLED_CANCELLED
}

class SafetyPinRepository private constructor(context: Context) {
    private val prefs = createEncryptedPrefs(context.applicationContext)
    private val _config = MutableStateFlow(readConfig())
    val config: StateFlow<SafetyPinConfig> = _config.asStateFlow()

    init {
        ensureDefaults()
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        refresh()
    }

    fun setPins(normalPin: String, duressPin: String, enabled: Boolean = _config.value.isEnabled): Boolean {
        val normal = normalPin.trim()
        val duress = duressPin.trim()
        if (!isValidPin(normal) || !isValidPin(duress) || normal == duress) return false

        prefs.edit()
            .putString(KEY_NORMAL_PIN_HASH, hashPin(normal))
            .putString(KEY_DURESS_PIN_HASH, hashPin(duress))
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        refresh()
        return true
    }

    fun verifyPin(pin: String): PinVerificationResult {
        val cleanPin = pin.trim()
        val current = _config.value
        if (!current.isEnabled) return PinVerificationResult.DISABLED_CANCELLED
        if (!isValidPin(cleanPin)) return PinVerificationResult.INVALID

        val cleanHash = hashPin(cleanPin)
        return when (cleanHash) {
            current.normalPin -> PinVerificationResult.NORMAL_CANCELLED
            current.duressPin -> PinVerificationResult.DURESS_ACTIVATED
            else -> PinVerificationResult.INVALID
        }
    }

    private fun ensureDefaults() {
        if (!prefs.contains(KEY_SALT)) {
            prefs.edit().putString(KEY_SALT, createSalt()).apply()
        }
        if (!prefs.contains(KEY_NORMAL_PIN_HASH) || !prefs.contains(KEY_DURESS_PIN_HASH)) {
            setPins(DEFAULT_NORMAL_PIN, DEFAULT_DURESS_PIN, enabled = true)
        } else {
            refresh()
        }
    }

    private fun readConfig(): SafetyPinConfig {
        return SafetyPinConfig(
            normalPin = prefs.getString(KEY_NORMAL_PIN_HASH, "").orEmpty(),
            duressPin = prefs.getString(KEY_DURESS_PIN_HASH, "").orEmpty(),
            isEnabled = prefs.getBoolean(KEY_ENABLED, true)
        )
    }

    private fun refresh() {
        _config.value = readConfig()
    }

    private fun hashPin(pin: String): String {
        val salt = prefs.getString(KEY_SALT, null) ?: createSalt().also {
            prefs.edit().putString(KEY_SALT, it).apply()
        }
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun createSalt(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun isValidPin(pin: String): Boolean {
        return pin.length in 4..6 && pin.all(Char::isDigit)
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        return EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val PREFS_NAME = "silent_safety_pin"
        private const val KEY_NORMAL_PIN_HASH = "normal_pin_hash"
        private const val KEY_DURESS_PIN_HASH = "duress_pin_hash"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SALT = "pin_salt"
        private const val DEFAULT_NORMAL_PIN = "1234"
        private const val DEFAULT_DURESS_PIN = "0000"

        @Volatile
        private var INSTANCE: SafetyPinRepository? = null

        fun getInstance(context: Context): SafetyPinRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SafetyPinRepository(context).also { INSTANCE = it }
            }
        }
    }
}
