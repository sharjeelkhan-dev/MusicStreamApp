package com.musicstream.app.service.audio_fx
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEffectManager @Inject constructor() {
    private val TAG = "AudioEffectManager"
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private var isInitialized = false
    private var lastBoundSessionId: Int = -1

    // Desired state to persist even if hardware fails or is re-initialized
    private var desiredPreset: String = "Flat"
    private var desiredBands: Map<Int, Int> = emptyMap()
    private var desiredBassBoost: Int = 0
    private var desiredVirtualizer: Int = 0

    // Dynamic hardware capabilities caching
    private var minBandLevel: Short = -1500
    private var maxBandLevel: Short = 1500

    @Synchronized
    fun onSessionIdChanged(sessionId: Int, force: Boolean = false) {
        if (!force && sessionId == lastBoundSessionId && isInitialized) {
            Log.d(TAG, "Already bound to session $sessionId. Skipping re-init.")
            return
        }

        Log.i(TAG, "Initializing hardware effects (Target: $sessionId, Previous: $lastBoundSessionId, Force: $force)")

        try {
            var success = initHardware(sessionId)

            // Fallback to Global Session 0 if local bonding fails
            if (!success && sessionId != 0) {
                Log.w(TAG, "Local bonding failed for $sessionId, falling back to Global Session 0")
                success = initHardware(0)
            }

            if (success) {
                lastBoundSessionId = if (sessionId > 0) sessionId else 0
                isInitialized = true
                syncDesiredStateToHardware()
                Log.i(TAG, "Audio Effects hardware successfully initialized and synced.")
            } else {
                Log.e(TAG, "Hardware persistently refused all bonding attempts.")
                isInitialized = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal Error during AudioEffect initialization: ${e.message}")
            isInitialized = false
        }
    }

    private fun initHardware(id: Int): Boolean {
        releaseInternal()

        return try {
            // Some devices need a tiny breather to free up native resources
            Thread.sleep(50)
            
            // Priority 0 is standard for most apps
            val priority = 0
            
            // Initialize Equalizer
            try {
                equalizer = Equalizer(priority, id)
                equalizer?.enabled = true
                Log.d(TAG, "Equalizer successfully linked to session $id")
            } catch (e: Exception) {
                Log.e(TAG, "Equalizer failed for session $id: ${e.message}")
            }

            // Initialize BassBoost
            try {
                bassBoost = BassBoost(priority, id)
                bassBoost?.enabled = true
                Log.d(TAG, "BassBoost initialized for session $id")
            } catch (e: Exception) {
                Log.e(TAG, "BassBoost failed: ${e.message}")
            }

            // Initialize Virtualizer
            try {
                virtualizer = Virtualizer(priority, id)
                virtualizer?.enabled = true
                Log.d(TAG, "Virtualizer initialized for session $id")
            } catch (e: Exception) {
                Log.e(TAG, "Virtualizer failed: ${e.message}")
            }

            // Verify hardware boundaries
            equalizer?.let {
                val range = it.bandLevelRange
                if (range != null && range.size >= 2) {
                    minBandLevel = range[0]
                    maxBandLevel = range[1]
                    Log.d(TAG, "Verified Hardware EQ range: $minBandLevel to $maxBandLevel")
                }
            }

            val success = equalizer != null || bassBoost != null || virtualizer != null
            Log.d(TAG, "Hardware Init Final Result: Success=$success")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Critical failure during hardware init: ${e.message}")
            releaseInternal()
            false
        }
    }

    @Synchronized
    private fun syncDesiredStateToHardware() {
        if (desiredPreset == "Custom") {
            applyBandsToHardware(desiredBands)
        } else {
            applyPresetToHardware(desiredPreset)
        }
        applyBassBoostToHardware(desiredBassBoost)
        applyVirtualizerToHardware(desiredVirtualizer)
    }

    @Synchronized
    fun setBassBoost(strength: Int) {
        desiredBassBoost = strength
        applyBassBoostToHardware(strength)
    }

    private fun applyBassBoostToHardware(strength: Int) {
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(strength.coerceIn(0, 1000).toShort())
                    Log.d(TAG, "Applied BassBoost hardware level: $strength")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying BassBoost: ${e.message}")
        }
    }

    @Synchronized
    fun setVirtualizer(strength: Int) {
        desiredVirtualizer = strength
        applyVirtualizerToHardware(strength)
    }

    private fun applyVirtualizerToHardware(strength: Int) {
        try {
            virtualizer?.let {
                if (it.strengthSupported) {
                    it.setStrength(strength.coerceIn(0, 1000).toShort())
                    Log.d(TAG, "Applied Virtualizer hardware level: $strength")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying Virtualizer: ${e.message}")
        }
    }

    @Synchronized
    fun setAllBands(bandLevels: Map<Int, Int>) {
        desiredPreset = "Custom"
        desiredBands = bandLevels
        applyBandsToHardware(bandLevels)
    }

    private fun applyBandsToHardware(bandLevels: Map<Int, Int>) {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            for (i in 0 until numBands) {
                bandLevels[i]?.let { level ->
                    // FIX 3: Safe dynamic clamp utilizing native device metrics
                    val safeLevel = level.coerceIn(minBandLevel.toInt(), maxBandLevel.toInt()).toShort()
                    eq.setBandLevel(i.toShort(), safeLevel)
                }
            }
            Log.d(TAG, "Applied Custom Bands to Hardware Matrix Successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying EQ Bands: ${e.message}")
        }
    }

    @Synchronized
    fun applyPreset(preset: String) {
        desiredPreset = preset
        applyPresetToHardware(preset)
    }

    private fun applyPresetToHardware(preset: String) {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            val levels = getLevelsForPreset(preset, numBands.toInt())
            levels.forEachIndexed { index, level ->
                if (index < numBands) {
                    val safeLevel = level.coerceIn(minBandLevel.toInt(), maxBandLevel.toInt()).toShort()
                    eq.setBandLevel(index.toShort(), safeLevel)
                }
            }
            Log.i(TAG, "Hardware Preset Applied: $preset")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying preset to hardware: ${e.message}")
        }
    }

    private fun getLevelsForPreset(preset: String, numBands: Int): List<Int> {
        val baseLevels = when (preset) {
            "Flat" -> listOf(0, 0, 0, 0, 0)
            "Bass Boost" -> listOf(1100, 700, 0, 0, 0)
            "Rock" -> listOf(700, 500, -200, 500, 800)
            "Pop" -> listOf(-200, 300, 700, 300, -200)
            "Electronic" -> listOf(800, 600, 0, 500, 800)
            "Classical" -> listOf(0, 0, 0, -500, -500)
            "Jazz" -> listOf(0, 0, 500, 500, 0)
            "Dance" -> listOf(900, 0, 500, 700, 0)
            else -> listOf(0, 0, 0, 0, 0)
        }

        return if (numBands == baseLevels.size) {
            baseLevels
        } else {
            List(numBands) { i ->
                val ratio = i.toDouble() / (numBands - 1).coerceAtLeast(1)
                val baseIdx = (ratio * (baseLevels.size - 1)).toInt()
                baseLevels[baseIdx]
            }
        }
    }

    @Synchronized
    fun release() {
        releaseInternal()
        lastBoundSessionId = -1
    }

    private fun releaseInternal() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (e: Exception) { }
        finally { equalizer = null }

        try {
            bassBoost?.enabled = false
            bassBoost?.release()
        } catch (e: Exception) { }
        finally { bassBoost = null }

        try {
            virtualizer?.enabled = false
            virtualizer?.release()
        } catch (e: Exception) { }
        finally { virtualizer = null }

        isInitialized = false
    }
}