package com.finance.app.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.concurrent.ConcurrentHashMap

/**
 * Icon cache utility for category icons to improve performance
 * Caches rendered icon data to avoid repeated calculations
 */
object IconCache {
    private val iconCache = ConcurrentHashMap<String, CachedIcon>()
    
    data class CachedIcon(
        val displayText: String,
        val colorInt: Int,
        val size: Float
    )
    
    /**
     * Get cached icon data or create and cache new one
     */
    fun getCachedIcon(
        iconName: String,
        color: String,
        size: Dp
    ): CachedIcon {
        val key = "${iconName}_${color}_${size.value}"
        
        return iconCache.getOrPut(key) {
            CachedIcon(
                displayText = iconName.firstOrNull()?.toString()?.uppercase() ?: "?",
                colorInt = parseColorSafe(color).toArgb(),
                size = size.value
            )
        }
    }
    
    /**
     * Clear the icon cache (useful for memory management)
     */
    fun clearCache() {
        iconCache.clear()
    }
    
    /**
     * Get cache size for debugging
     */
    fun getCacheSize(): Int = iconCache.size
    
    private fun parseColorSafe(colorString: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: Exception) {
            Color.Gray
        }
    }
}

/**
 * Composable function to get cached icon with remember optimization
 */
@Composable
fun rememberCachedIcon(
    iconName: String,
    color: String,
    size: Dp = 48.dp
): IconCache.CachedIcon {
    return remember(iconName, color, size) {
        IconCache.getCachedIcon(iconName, color, size)
    }
}

/**
 * Extension function to get display text from cached icon
 */
fun IconCache.CachedIcon.getDisplayText(): String = displayText

/**
 * Extension function to get color from cached icon
 */
fun IconCache.CachedIcon.getColor(): Color = Color(colorInt)