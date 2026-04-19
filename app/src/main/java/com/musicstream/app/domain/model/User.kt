package com.musicstream.app.domain.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String = "",
    val isPremium: Boolean = true,
    val songCount: Int = 247,
    val playlistCount: Int = 12,
    val followingCount: Int = 89,
    val followersCount: Int = 142
)
