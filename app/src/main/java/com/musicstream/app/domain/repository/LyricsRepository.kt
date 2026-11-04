package com.musicstream.app.domain.repository

import com.musicstream.app.domain.model.LyricLine
import com.musicstream.app.domain.model.Song

interface LyricsRepository {
    suspend fun getSyncedLyrics(song: Song): List<LyricLine>
    suspend fun getPlainLyrics(song: Song): String?
}
