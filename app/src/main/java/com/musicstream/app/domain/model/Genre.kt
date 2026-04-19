package com.musicstream.app.domain.model

data class Genre(
    val id: String,
    val name: String,
    val gradientKey: String = name.lowercase()
)
