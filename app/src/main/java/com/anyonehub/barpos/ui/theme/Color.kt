// Copyright 2024 anyone-Hub
@file:Suppress("unused")

package com.anyonehub.barpos.ui.theme

import androidx.compose.ui.graphics.Color

// --- The "MidTown" Palette ---

// 1. Backgrounds & Surfaces
val MidtownOnyx       = Color(0xFF121212) // Deep Black (Main Background)
val MidtownSurface    = Color(0xFF1E1E1E) // Darker Grey (Cards/Dialogs)
val MidtownSurfaceVar = Color(0xFF2C2C2C) // Lighter Grey (Inactive Tabs/Fields)

// 2. Primary Branding (Emerald Green)
val MidtownEmerald     = Color(0xFF2FA192) // Primary Action Color
val MidtownEmeraldDark = Color(0xFF147265) // Container Backgrounds

// 3. Secondary/Accents (Gold & Amber)
val MidtownGold    = Color(0xFFFFD600) // Secondary Actions/Highlights
val MidtownAmber   = Color(0xFFFFAB00) // Warnings/Warning Containers

// 4. Feedback & Text
val MidtownError   = Color(0xFFCF6679) // Material Standard Error for Dark Mode
val MidtownText    = Color(0xFFEEEEEE) // Off-White (High Readability)
val MidtownTextDim = Color(0xFF9E9E9E) // Muted Text for IDs/Dates

// --- Semantic Mappings for Clarity ---
val BrandPrimary = MidtownEmerald
val BrandSecondary = MidtownGold
val BrandBackground = MidtownOnyx