package com.musicstream.app.presentation.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.data.MockData
import com.musicstream.app.domain.model.Artist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistsUiState(
    val artists: List<Artist> = emptyList(),
    val favoriteSongs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val selectedFilter: String = "All",
    val isShowingLikedSongs: Boolean = false
)

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistsUiState())
    val uiState: StateFlow<ArtistsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            musicRepository.getFavorites().collect { favs ->
                _uiState.update { it.copy(
                    artists = MockData.artists,
                    favoriteSongs = favs,
                    isLoading = false
                ) }
            }
        }
    }

    fun toggleShowingLikedSongs() {
        _uiState.update { it.copy(isShowingLikedSongs = !it.isShowingLikedSongs) }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(song)
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }
}
