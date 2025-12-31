package com.finance.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light color scheme for Finance App following Material 3 guidelines
 */
private val FinanceLightColorScheme = lightColorScheme(
    primary = FinanceGreen40,
    onPrimary = Color.White,
    primaryContainer = FinanceGreenContainer40,
    onPrimaryContainer = Color(0xFF002110),
    
    secondary = FinanceBlue40,
    onSecondary = Color.White,
    secondaryContainer = FinanceBlueContainer40,
    onSecondaryContainer = Color(0xFF001D36),
    
    tertiary = Color(0xFF6750A4),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEADDFF),
    onTertiaryContainer = Color(0xFF21005D),
    
    error = FinanceRed40,
    onError = Color.White,
    errorContainer = FinanceRedContainer40,
    onErrorContainer = Color(0xFF410002),
    
    background = Neutral99,
    onBackground = Neutral10,
    
    surface = Surface,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    
    surfaceTint = FinanceGreen40,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    inversePrimary = FinanceGreen80,
    
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
)

/**
 * Dark color scheme for Finance App following Material 3 guidelines
 */
private val FinanceDarkColorScheme = darkColorScheme(
    primary = FinanceGreen80,
    onPrimary = Color(0xFF003919),
    primaryContainer = FinanceGreenContainer80,
    onPrimaryContainer = FinanceGreenContainer40,
    
    secondary = FinanceBlue80,
    onSecondary = Color(0xFF003258),
    secondaryContainer = FinanceBlueContainer80,
    onSecondaryContainer = FinanceBlueContainer40,
    
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF381E72),
    tertiaryContainer = Color(0xFF4F378B),
    onTertiaryContainer = Color(0xFFEADDFF),
    
    error = FinanceRed80,
    onError = Color(0xFF690005),
    errorContainer = FinanceRedContainer80,
    onErrorContainer = FinanceRedContainer40,
    
    background = Color(0xFF0F1311),
    onBackground = Neutral90,
    
    surface = SurfaceDark,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    
    surfaceTint = FinanceGreen80,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = FinanceGreen40,
    
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
)

/**
 * Finance App theme with Material 3 design system
 * 
 * @param darkTheme Whether to use dark theme
 * @param dynamicColor Whether to use dynamic colors (Android 12+)
 * @param content The composable content
 */
@Composable
fun FinanceAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FinanceDarkColorScheme
        else -> FinanceLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FinanceTypography,
        shapes = FinanceShapes,
        content = content
    )
}

/**
 * Finance App theme with theme manager support
 * 
 * @param themeState Current theme state from ThemeManager
 * @param content The composable content
 */
@Composable
fun FinanceAppTheme(
    themeState: ThemeState,
    content: @Composable () -> Unit
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeState.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemInDarkTheme
    }
    
    FinanceAppTheme(
        darkTheme = darkTheme,
        dynamicColor = themeState.isDynamicColorEnabled,
        content = content
    )
}
