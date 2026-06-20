package com.safepulse.service

import android.content.Context
import android.media.AudioManager
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.ArrayDeque

/**
 * Keeps a foreground-service owned MediaSession active so hardware volume
 * shortcuts can still be detected while the screen is off on supported devices.
 */
class VolumeButtonEmergencyTrigger(
    context: Context,
    private val scope: CoroutineScope,
    private val onTripleVolumeUp: () -> Unit,
    private val onTripleVolumeDown: () -> Unit
) {
    companion object {
        private const val TAG = "VolumeButtonEmergency"
        private const val PRESS_WINDOW_MS = 2_000L
        private const val MIN_PRESS_INTERVAL_MS = 120L
        private const val TRIGGER_COOLDOWN_MS = 1_500L
    }

    private val appContext = context.applicationContext
    private val upPresses = ArrayDeque<Long>()
    private val downPresses = ArrayDeque<Long>()
    private var mediaSession: MediaSession? = null
    private var lastTriggerAt = 0L

    fun start() {
        if (mediaSession != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return

        val volumeProvider = object : VolumeProvider(
            VOLUME_CONTROL_RELATIVE,
            100,
            50
        ) {
            override fun onAdjustVolume(direction: Int) {
                handleVolumeDirection(direction)
            }
        }

        mediaSession = MediaSession(appContext, "SafePulseVolumeShortcut").apply {
            setPlaybackToRemote(volumeProvider)
            setPlaybackState(
                PlaybackState.Builder()
                    .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                    .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE)
                    .build()
            )
            @Suppress("DEPRECATION")
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS)
            isActive = true
        }

        Log.i(TAG, "Volume button emergency shortcuts armed")
    }

    fun stop() {
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        upPresses.clear()
        downPresses.clear()
        Log.d(TAG, "Volume button emergency shortcuts stopped")
    }

    private fun handleVolumeDirection(direction: Int) {
        when {
            direction == AudioManager.ADJUST_RAISE || direction > 0 -> {
                recordPress(upPresses, downPresses, "volume up", onTripleVolumeUp)
            }
            direction == AudioManager.ADJUST_LOWER || direction < 0 -> {
                recordPress(downPresses, upPresses, "volume down", onTripleVolumeDown)
            }
        }
    }

    private fun recordPress(
        targetPresses: ArrayDeque<Long>,
        oppositePresses: ArrayDeque<Long>,
        label: String,
        callback: () -> Unit
    ) {
        val now = System.currentTimeMillis()
        val previousPress = targetPresses.peekLast()
        if (previousPress != null && now - previousPress < MIN_PRESS_INTERVAL_MS) return

        targetPresses.addLast(now)
        oppositePresses.clear()

        while (targetPresses.isNotEmpty() && now - targetPresses.peekFirst() > PRESS_WINDOW_MS) {
            targetPresses.removeFirst()
        }

        Log.d(TAG, "$label pressed (${targetPresses.size}/3)")

        if (targetPresses.size >= 3 && now - lastTriggerAt > TRIGGER_COOLDOWN_MS) {
            targetPresses.clear()
            oppositePresses.clear()
            lastTriggerAt = now
            Log.i(TAG, "Triple $label detected")
            scope.launch { callback() }
        }
    }
}
