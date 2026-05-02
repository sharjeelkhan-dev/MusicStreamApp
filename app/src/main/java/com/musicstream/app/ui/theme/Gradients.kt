package com.musicstream.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Gradients {
    // Featured Card - Yellow to Orange
    val featured = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFFD700), Color(0xFFFF8C00))
    )

    // Genre card gradients
    val pop = Brush.linearGradient(
        colors = listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val hipHop = Brush.linearGradient(
        colors = listOf(Color(0xFFFF6B6B), Color(0xFFFFA07A)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val electronic = Brush.linearGradient(
        colors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val rnb = Brush.linearGradient(
        colors = listOf(Color(0xFFEC4899), Color(0xFFA855F7)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val rock = Brush.linearGradient(
        colors = listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val classical = Brush.linearGradient(
        colors = listOf(Color(0xFF10B981), Color(0xFF6EE7B7)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val jazz = Brush.linearGradient(
        colors = listOf(Color(0xFFF97316), Color(0xFFFBBF24)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val lofi = Brush.linearGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // Trending card gradients (matching screenshot thumbnails)
    val trendingPink = Brush.linearGradient(
        colors = listOf(Color(0xFFE879A8), Color(0xFFF9A8C9)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val trendingPurple = Brush.linearGradient(
        colors = listOf(Color(0xFF9F7AEA), Color(0xFFB794F4)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val trendingOrange = Brush.linearGradient(
        colors = listOf(Color(0xFFF6AD55), Color(0xFFED8936)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // Playlist card gradients
    val playlistBlue = Brush.linearGradient(
        colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val playlistPink = Brush.linearGradient(
        colors = listOf(Color(0xFFEC4899), Color(0xFFD946EF)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val playlistGreen = Brush.linearGradient(
        colors = listOf(Color(0xFF10B981), Color(0xFF059669)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // Profile banner gradient
    val profileBanner = Brush.horizontalGradient(
        colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFFF8C00))
    )

    // Song item thumbnail gradients
    val songThumbPink = Brush.linearGradient(
        colors = listOf(Color(0xFFF472B6), Color(0xFFEC4899))
    )

    val songThumbOrange = Brush.linearGradient(
        colors = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
    )

    val songThumbBlue = Brush.linearGradient(
        colors = listOf(Color(0xFF60A5FA), Color(0xFF3B82F6))
    )

    val songThumbGreen = Brush.linearGradient(
        colors = listOf(Color(0xFF34D399), Color(0xFF10B981))
    )

    // Fallback colors for instant dynamic UI
    val songThumbColors = listOf(
        Color(0xFFEC4899), // Pink
        Color(0xFFF59E0B), // Orange
        Color(0xFF3B82F6), // Blue
        Color(0xFF10B981), // Green
        Color(0xFF9F7AEA)  // Purple
    )

    fun getGenreGradient(genre: String): Brush {
        return when (genre.lowercase()) {
            "pop" -> pop
            "hip-hop" -> hipHop
            "electronic" -> electronic
            "r&b" -> rnb
            "rock" -> rock
            "classical" -> classical
            "jazz" -> jazz
            "lo-fi" -> lofi
            else -> pop
        }
    }
}
