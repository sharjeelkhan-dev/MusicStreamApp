package com.attendance.app.presentation.theme

import androidx.compose.ui.graphics.Color

// Primary Green Palette (matching the dark green header in screenshots)
val PrimaryGreen = Color(0xFF2E7D52)
val PrimaryGreenDark = Color(0xFF1B5E3B)
val PrimaryGreenLight = Color(0xFF4CAF7D)
val PrimaryGreenSurface = Color(0xFFE8F5E9)

// Surface / Background
val SurfaceLight = Color(0xFFF5F5F5)
val SurfaceDark = Color(0xFF1A1C1A)
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF222522)
val BackgroundLight = Color(0xFFF8F9FA)
val BackgroundDark = Color(0xFF0F110F) // Deep green-tinted black

// Text Colors
val TextPrimaryLight = Color(0xFF1A1A1A)
val TextSecondaryLight = Color(0xFF6B7280)
val TextPrimaryDark = Color(0xFFE1E3E1)
val TextSecondaryDark = Color(0xFF9CA3AF)

// Status Colors
val PresentGreen = Color(0xFF36B26E)
val PresentGreenBg = Color(0xFFE8F5E9)
val AbsentRed = Color(0xFFE54440)
val AbsentRedBg = Color(0xFFFFEBEE)
val LateOrange = Color(0xFFFFA726)
val LateOrangeBg = Color(0xFFFFF3E0)

// Attendance Percentage Colors
val PercentageHigh = Color(0xFF4CAF50)   // 80-100%
val PercentageMedium = Color(0xFFFFA726) // 50-79%
val PercentageLow = Color(0xFFE53935)    // 0-49%

// Avatar Colors (Dark/Vibrant palette)
val AvatarColors = listOf(
    Color(0xFF1B5E20), // Dark Green
    Color(0xFF0D47A1), // Dark Blue
    Color(0xFFB71C1C), // Dark Red
    Color(0xFF4A148C), // Dark Purple
    Color(0xFF004D40), // Dark Teal
    Color(0xFFE65100), // Dark Orange
    Color(0xFF3E2723), // Dark Brown
    Color(0xFF212121), // Dark Grey
    Color(0xFF827717), // Dark Lime
    Color(0xFF006064), // Dark Cyan
)

val AvatarTextColor = Color.White

fun getAvatarColor(name: String): Color {
    if (name.isEmpty()) return AvatarColors[0]
    val index = Math.abs(name.hashCode()) % AvatarColors.size
    return AvatarColors[index]
}

// Bottom Nav
val BottomNavSelected = PrimaryGreen
val BottomNavUnselected = Color(0xFF9E9E9E)
val BottomNavSelectedDark = PrimaryGreenLight
val BottomNavUnselectedDark = Color(0xFF757575)

// Divider
val DividerColor = Color(0xFFE0E0E0)
val DividerColorDark = Color(0xFF2C2C2C)
