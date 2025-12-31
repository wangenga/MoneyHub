package com.finance.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Shape system for Finance App
 * Following Material Design 3 shape guidelines
 */
val FinanceShapes = Shapes(
    // Extra Small - for small components like chips, badges
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small - for buttons, cards
    small = RoundedCornerShape(8.dp),
    
    // Medium - for larger cards, dialogs
    medium = RoundedCornerShape(12.dp),
    
    // Large - for bottom sheets, large surfaces
    large = RoundedCornerShape(16.dp),
    
    // Extra Large - for very large surfaces
    extraLarge = RoundedCornerShape(28.dp)
)