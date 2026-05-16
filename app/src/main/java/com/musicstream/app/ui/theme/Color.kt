package com.musicstream.app.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Background
val DarkBackground = Color(0xFF000000) // Pure Black
val DarkSurface = Color(0xFF0F0F0F) // Very Dark Gray for surface
val DarkCardSurface = Color(0xFF1A1A1A) // Slightly lighter for cards
val DarkElevated = Color(0xFF242424) // More lighter for elevated items

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8B8BA3)
val TextTertiary = Color(0xFF5C5C7A)

// Accent Colors
val AccentOrange = Color(0xFFFF8C00)
val AccentAmber = Color(0xFFFFA500)
val AccentYellow = Color(0xFFFFD700)
val AccentPurple = Color(0xFF1E1E2F)
val AccentPink = Color(0xFFEC4899)
val AccentMagenta = Color(0xFFD946EF)
val AccentGreen = Color(0xFF10B981)
val AccentCyan = Color(0xFF06B6D4)
val AccentBlue = Color(0xFF3B82F6)
val AccentRed = Color(0xFFEF4444)
val AccentTeal = Color(0xFF14B8A6)

// Premium Badge
val PremiumGold = Color(0xFFFFD700)

// Bottom Nav
val NavBarBackground = Color(0xFF000000)
val NavBarInactive = Color(0xFF5C5C7A)
val NavBarActive = Color.Black // This will be used as a base, logic in BottomNavBar.kt should handle theme properly

// Favorite Heart
val FavoriteRed = Color(0xFFFF4D6A)
val FavoriteInactive = Color(0xFF5C5C7A)

// Seek Bar / Progress
val SeekBarActive = AccentOrange
val SeekBarInactive = Color(0xFF2D2A45)

// Featured Card Gradient
val FeaturedGradientStart = Color(0xFFFFD700)
val FeaturedGradientEnd = Color(0xFFFF8C00)

// Sign Out Button
val SignOutRed = Color(0xFFFF4D6A)
val SignOutBorder = Color(0xFF3D1F30)
