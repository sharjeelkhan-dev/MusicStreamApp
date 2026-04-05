package com.attendance.app.presentation.theme

import androidx.compose.ui.graphics.Color

// Primary Green Palette (matching the dark green header in screenshots)
val PrimaryGreen = Color(0xFF2E7D52)
val PrimaryGreenDark = Color(0xFF1B5E3B)
val PrimaryGreenLight = Color(0xFF4CAF7D)
val PrimaryGreenSurface = Color(0xFFE8F5E9)

// Surface / Background
val SurfaceLight = Color(0xFFF5F5F5)
val SurfaceDark = Color(0xFF121212)
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF1E1E1E)
val BackgroundLight = Color(0xFFF8F9FA)
val BackgroundDark = Color(0xFF0D0D0D)

// Text Colors
val TextPrimaryLight = Color(0xFF1A1A1A)
val TextSecondaryLight = Color(0xFF6B7280)
val TextPrimaryDark = Color(0xFFE0E0E0)
val TextSecondaryDark = Color(0xFF9CA3AF)

// Status Colors
val PresentGreen = Color(0xFF4CAF50)
val PresentGreenBg = Color(0xFFE8F5E9)
val AbsentRed = Color(0xFFE53935)
val AbsentRedBg = Color(0xFFFFEBEE)
val LateOrange = Color(0xFFFFA726)
val LateOrangeBg = Color(0xFFFFF3E0)

// Attendance Percentage Colors
val PercentageHigh = Color(0xFF4CAF50)   // 80-100%
val PercentageMedium = Color(0xFFFFA726) // 50-79%
val PercentageLow = Color(0xFFE53935)    // 0-49%

// Avatar Colors (rotating for different students)
val AvatarColors = listOf(
    Color(0xFF3F51B5), // Indigo
    Color(0xFF009688), // Teal
    Color(0xFFFF5722), // Deep Orange
    Color(0xFF795548), // Brown
    Color(0xFF607D8B), // Blue Grey
    Color(0xFF9C27B0), // Purple
    Color(0xFFE91E63), // Pink
    Color(0xFF00BCD4), // Cyan
    Color(0xFF4CAF50), // Green
    Color(0xFFFF9800), // Orange
)

fun getAvatarColor(name: String): Color {
    if (name.isEmpty()) return AvatarColors[0]
    val index = Math.abs(name.hashCode()) % AvatarColors.size
    return AvatarColors[index]
}

// Bottom Nav
val BottomNavSelected = PrimaryGreen
val BottomNavUnselected = Color(0xFF9E9E9E)

// Divider
val DividerColor = Color(0xFFE0E0E0)
val DividerColorDark = Color(0xFF2C2C2C)
