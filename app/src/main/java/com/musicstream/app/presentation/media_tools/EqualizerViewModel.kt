package com.musicstream.app.presentation.media_tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    init {
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
        viewModelScope.launch {
            settingsRepository.setEqualizerPreset(preset)
        }
    }

    fun setBassBoost(level: Int) {
        viewModelScope.launch {
            settingsRepository.setBassBoostLevel(level)
        }
    }

    fun setVirtualizer(level: Int) {
        viewModelScope.launch {
            settingsRepository.setVirtualizerLevel(level)
        }
    }

    fun setBandLevel(band: Int, level: Int) {
        viewModelScope.launch {
            settingsRepository.setEqualizerPreset("Custom")
            settingsRepository.setEqualizerBandLevel(band, level)
        }
    }
}
