package com.musicstream.app.domain.model

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String = "",
    val followerCount: String = "0",
    val songCount: Int = 0
)
