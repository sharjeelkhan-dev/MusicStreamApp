package com.musicstream.app.domain.model

data class Playlist(
    val id: String,
    val name: String,
    val songCount: Int = 0,
    val coverUrl: String = "",
    val gradientIndex: Int = 0,
    val songs: List<Song> = emptyList()
)
