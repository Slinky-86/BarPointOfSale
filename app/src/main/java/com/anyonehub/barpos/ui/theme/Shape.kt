// Copyright 2024 anyone-Hub
package com.anyonehub.barpos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Used for Tooltips/Small Indicators
    small = RoundedCornerShape(4.dp),
    // Standard POS Buttons and Menu Cards
    medium = RoundedCornerShape(8.dp),
    // Dialogs, Sidebars, and the Receipt Panel
    large = RoundedCornerShape(16.dp)
)