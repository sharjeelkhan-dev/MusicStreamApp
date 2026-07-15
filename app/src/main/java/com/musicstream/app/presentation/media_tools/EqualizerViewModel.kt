package com.musicstream.app.presentation.media_tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EqualizerUiState(
    val currentPreset: String = "Flat",
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val bandLevels: Map<Int, Int> = emptyMap(),
    val presets: List<String> = listOf("Flat", "Bass Boost", "Rock", "Pop", "Electronic", "Classical", "Jazz", "Dance", "Custom")
)

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EqualizerUiState())
    val uiState = _uiState.asStateFlow()

    private val saveJobs = mutableMapOf<String, Job>()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.getEqualizerPreset(),
                settingsRepository.getBassBoostLevel(),
                settingsRepository.getVirtualizerLevel(),
                settingsRepository.getEqualizerBandLevels()
            ) { preset, bass, virtual, bands ->
                EqualizerUiState(
                    currentPreset = preset,
                    bassBoost = bass,
                    virtualizer = virtual,
                    bandLevels = bands
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun setPreset(preset: String) {
        // Immediate UI feedback
        _uiState.update { it.copy(currentPreset = preset) }
        
        // Immediate persistence for presets to trigger Service sync
        viewModelScope.launch {
            settingsRepository.setEqualizerPreset(preset)
        }
    }

    fun setBassBoost(level: Int) {
        // Immediate UI feedback
        _uiState.update { it.copy(bassBoost = level) }
        
        // Debounce actual persistence for smoothness
        debounceSave("bass_boost") {
            settingsRepository.setBassBoostLevel(level)
        }
    }

    fun setVirtualizer(level: Int) {
        // Immediate UI feedback
        _uiState.update { it.copy(virtualizer = level) }
        
        debounceSave("virtualizer") {
            settingsRepository.setVirtualizerLevel(level)
        }
    }

    fun setBandLevel(band: Int, level: Int) {
        // Optimistic UI update
        val updatedBands = _uiState.value.bandLevels.toMutableMap().apply {
            put(band, level)
        }
        
        _uiState.update { 
            it.copy(
                bandLevels = updatedBands,
                currentPreset = "Custom"
            ) 
        }
        
        // Debounce band changes
        debounceSave("band_$band") {
            settingsRepository.setEqualizerPreset("Custom")
            settingsRepository.setEqualizerBandLevel(band, level)
        }
    }

    private fun debounceSave(key: String, action: suspend () -> Unit) {
        saveJobs[key]?.cancel()
        saveJobs[key] = viewModelScope.launch {
            delay(150) // Balanced delay for responsiveness vs database overhead
            action()
        }
    }
}
