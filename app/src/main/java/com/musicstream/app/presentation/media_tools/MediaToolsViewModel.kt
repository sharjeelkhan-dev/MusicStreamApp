package com.musicstream.app.presentation.media_tools

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class MediaToolsUiState(
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class MediaToolsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaToolsUiState())
    val uiState = _uiState.asStateFlow()

    private val _onConversionComplete = MutableSharedFlow<Song>()
    val onConversionComplete = _onConversionComplete.asSharedFlow()

    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    fun convertVideoToAudio(videoUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = "Extracting Audio...", error = null) }
            
            val inputPath = getPathFromUri(videoUri) ?: run {
                _uiState.update { it.copy(isProcessing = false) }
                _message.emit("Invalid Video File")
                return@launch
            }

            val outputDir = File(context.getExternalFilesDir(null), "ConvertedAudio")
            if (!outputDir.exists()) outputDir.mkdirs()
            
            val timestamp = System.currentTimeMillis()
            val outputFileName = "extracted_$timestamp.mp3"
            val outputPath = File(outputDir, outputFileName).absolutePath

            // FFmpeg command to extract audio: -i input -vn -acodec libmp3lame -q:a 2 output
            val command = "-i \"$inputPath\" -vn -acodec libmp3lame -q:a 2 \"$outputPath\""

            FFmpegKit.executeAsync(command) { session ->
                val returnCode = session.returnCode
                if (ReturnCode.isSuccess(returnCode)) {
                    val newSong = Song(
                        id = "local_$timestamp",
                        title = "Extracted Audio $timestamp",
                        artist = "Converted",
                        localPath = outputPath,
                        duration = session.duration,
                        gradientIndex = (timestamp.toInt() and Integer.MAX_VALUE) % 5
                    )
                    
                    viewModelScope.launch {
                        musicRepository.addLocalSong(newSong)
                        _onConversionComplete.emit(newSong)
                        _uiState.update { it.copy(isProcessing = false) }
                        _message.emit("Audio Extracted Successfully!")
                    }
                } else {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isProcessing = false) }
                        _message.emit("Extraction Failed. Error code: $returnCode")
                    }
                }
            }
        }
    }

    fun applyEqualizer(inputUri: Uri, preset: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = "Applying $preset EQ...") }
            
            val inputPath = getPathFromUri(inputUri) ?: run {
                _uiState.update { it.copy(isProcessing = false) }
                _message.emit("Invalid Audio File")
                return@launch
            }

            val timestamp = System.currentTimeMillis()
            val outputDir = File(context.getExternalFilesDir(null), "ConvertedAudio")
            if (!outputDir.exists()) outputDir.mkdirs()
            val outputPath = File(outputDir, "eq_${preset.lowercase().replace(" ", "_")}_$timestamp.mp3").absolutePath

            // FFmpeg command for 10-band equalizer
            // Presets mapping to gain values
            val gains = when (preset.lowercase()) {
                "bass boost" -> "g=5:g=4:g=2:g=0:g=0:g=0:g=0:g=0:g=0:g=0"
                "treble boost" -> "g=0:g=0:g=0:g=0:g=0:g=0:g=2:g=4:g=5:g=6"
                "vocal boost" -> "g=-2:g=-2:g=0:g=3:g=4:g=4:g=2:g=0:g=0:g=0"
                else -> "g=0:g=0:g=0:g=0:g=0:g=0:g=0:g=0:g=0:g=0"
            }

            val filter = "equalizer=f=31:width_type=o:width=1:${gains.split(":")[0]}," +
                         "equalizer=f=62:width_type=o:width=1:${gains.split(":")[1]}," +
                         "equalizer=f=125:width_type=o:width=1:${gains.split(":")[2]}," +
                         "equalizer=f=250:width_type=o:width=1:${gains.split(":")[3]}," +
                         "equalizer=f=500:width_type=o:width=1:${gains.split(":")[4]}," +
                         "equalizer=f=1000:width_type=o:width=1:${gains.split(":")[5]}," +
                         "equalizer=f=2000:width_type=o:width=1:${gains.split(":")[6]}," +
                         "equalizer=f=4000:width_type=o:width=1:${gains.split(":")[7]}," +
                         "equalizer=f=8000:width_type=o:width=1:${gains.split(":")[8]}," +
                         "equalizer=f=16000:width_type=o:width=1:${gains.split(":")[9]}"

            val command = "-i \"$inputPath\" -af \"$filter\" \"$outputPath\""

            FFmpegKit.executeAsync(command) { session ->
                if (ReturnCode.isSuccess(session.returnCode)) {
                    val newSong = Song(
                        id = "eq_$timestamp",
                        title = "EQ Applied ($preset)",
                        artist = "Converted",
                        localPath = outputPath,
                        duration = session.duration,
                        gradientIndex = (timestamp.toInt() and Integer.MAX_VALUE) % 5
                    )
                    viewModelScope.launch {
                        musicRepository.addLocalSong(newSong)
                        _onConversionComplete.emit(newSong)
                        _uiState.update { it.copy(isProcessing = false) }
                        _message.emit("EQ Applied Successfully!")
                    }
                } else {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isProcessing = false) }
                        _message.emit("EQ Application Failed")
                    }
                }
            }
        }
    }

    fun reduceNoise(inputUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = "Reducing Noise...") }
            val inputPath = getPathFromUri(inputUri) ?: return@launch
            val timestamp = System.currentTimeMillis()
            val outputPath = File(context.cacheDir, "denoised_$timestamp.mp3").absolutePath

            // FFmpeg command for noise reduction: afftdn (Audio FFT Denoise)
            val command = "-i \"$inputPath\" -af \"afftdn=nr=12:nt=w\" \"$outputPath\""

            FFmpegKit.executeAsync(command) { session ->
                if (ReturnCode.isSuccess(session.returnCode)) {
                    val newSong = Song(
                        id = "denoised_$timestamp",
                        title = "Denoised Audio",
                        artist = "Converted",
                        localPath = outputPath,
                        duration = session.duration,
                        gradientIndex = (timestamp.toInt() and Integer.MAX_VALUE) % 5
                    )
                    viewModelScope.launch {
                        musicRepository.addLocalSong(newSong)
                        _onConversionComplete.emit(newSong)
                        _uiState.update { it.copy(isProcessing = false) }
                        _message.emit("Noise Reduced Successfully!")
                    }
                } else {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isProcessing = false) }
                        _message.emit("Noise Reduction Failed")
                    }
                }
            }
        }
    }

    fun updateMetadata(inputUri: Uri, title: String, artist: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = "Updating Tags...") }
            val inputPath = getPathFromUri(inputUri) ?: return@launch
            val timestamp = System.currentTimeMillis()
            val outputPath = File(context.cacheDir, "tagged_$timestamp.mp3").absolutePath

            // FFmpeg command to update metadata without re-encoding
            val command = "-i \"$inputPath\" -metadata title=\"$title\" -metadata artist=\"$artist\" -codec copy \"$outputPath\""

            FFmpegKit.executeAsync(command) { session ->
                if (ReturnCode.isSuccess(session.returnCode)) {
                    val newSong = Song(
                        id = "tagged_$timestamp",
                        title = title,
                        artist = artist,
                        localPath = outputPath,
                        duration = session.duration,
                        gradientIndex = (timestamp.toInt() and Integer.MAX_VALUE) % 5
                    )
                    viewModelScope.launch {
                        musicRepository.addLocalSong(newSong)
                        _onConversionComplete.emit(newSong)
                        _uiState.update { it.copy(isProcessing = false) }
                        _message.emit("Tags Updated Successfully!")
                    }
                } else {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isProcessing = false) }
                        _message.emit("Tag Update Failed")
                    }
                }
            }
        }
    }

    fun boostVolume(inputUri: Uri, factor: Float) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, statusMessage = "Boosting Volume...") }
            
            val inputPath = getPathFromUri(inputUri) ?: run {
                _uiState.update { it.copy(isProcessing = false) }
                _message.emit("Invalid Audio File")
                return@launch
            }
            val timestamp = System.currentTimeMillis()
            val outputPath = File(context.cacheDir, "boosted_$timestamp.mp3").absolutePath

            // FFmpeg command for volume boost: volume=2.0
            val command = "-i \"$inputPath\" -filter:a \"volume=$factor\" \"$outputPath\""

            FFmpegKit.executeAsync(command) { session ->
                if (ReturnCode.isSuccess(session.returnCode)) {
                    val newSong = Song(
                        id = "boosted_$timestamp",
                        title = "Boosted Audio $timestamp",
                        artist = "Converted",
                        localPath = outputPath,
                        duration = session.duration,
                        gradientIndex = (timestamp.toInt() and Integer.MAX_VALUE) % 5
                    )

                    viewModelScope.launch {
                        musicRepository.addLocalSong(newSong)
                        _onConversionComplete.emit(newSong)
                        _uiState.update { it.copy(isProcessing = false) }
                        _message.emit("Volume Boosted Successfully!")
                    }
                } else {
                    viewModelScope.launch {
                        _uiState.update { it.copy(isProcessing = false) }
                        _message.emit("Failed to boost volume")
                    }
                }
            }
        }
    }

    private fun getPathFromUri(uri: Uri): String? {
        // Simplified for this context. In a real app, use a proper content resolver utility.
        return try {
            val file = File(context.cacheDir, "temp_media_${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
    
    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null, error = null) }
    }
}
