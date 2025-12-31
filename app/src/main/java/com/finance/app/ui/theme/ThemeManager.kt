package com.finance.app.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Theme modes supported by the app
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Extension property to create DataStore for theme preferences
 */
private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

/**
 * Manager for theme-related preferences and state
 */
@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeDataStore = context.themeDataStore
    
    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    }
    
    /**
     * Flow of current theme mode
     */
    val themeMode: Flow<ThemeMode> = themeDataStore.data.map { preferences ->
        val themeModeString = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(themeModeString)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }
    
    /**
     * Flow of dynamic color preference
     */
    val isDynamicColorEnabled: Flow<Boolean> = themeDataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR_KEY] ?: true
    }
    
    /**
     * Set the theme mode
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        themeDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }
    
    /**
     * Set dynamic color preference
     */
    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        themeDataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }
}

/**
 * Composable that provides theme state
 */
@Composable
fun rememberThemeState(themeManager: ThemeManager): ThemeState {
    val themeMode by themeManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val isDynamicColorEnabled by themeManager.isDynamicColorEnabled.collectAsState(initial = true)
    
    return ThemeState(
        themeMode = themeMode,
        isDynamicColorEnabled = isDynamicColorEnabled
    )
}

/**
 * Data class representing current theme state
 */
data class ThemeState(
    val themeMode: ThemeMode,
    val isDynamicColorEnabled: Boolean
)