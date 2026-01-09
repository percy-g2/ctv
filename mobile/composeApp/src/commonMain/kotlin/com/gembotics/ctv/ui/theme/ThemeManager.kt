package com.gembotics.ctv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

enum class ThemeMode {
    Light,
    Dark,
    System
}

// Local theme mode provider
val LocalThemeMode = compositionLocalOf<ThemeMode> { ThemeMode.System }

// Theme state manager
object ThemeState {
    var themeMode = mutableStateOf(ThemeMode.System)
        private set
    
    fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }
}

@Composable
fun getColorScheme(themeMode: ThemeMode): ColorScheme {
    val useDarkTheme = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    
    return if (useDarkTheme) {
        // Dark theme: Navy blue base with darker backgrounds
        darkColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF4A90E2), // Bright blue accent
            onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            primaryContainer = androidx.compose.ui.graphics.Color(0xFF1E3A5F), // Navy blue container
            onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFE3F2FD),
            secondary = androidx.compose.ui.graphics.Color(0xFF6BB6FF), // Lighter blue for secondary
            onSecondary = androidx.compose.ui.graphics.Color(0xFF000000),
            secondaryContainer = androidx.compose.ui.graphics.Color(0xFF2C4A6B),
            onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFE3F2FD),
            tertiary = androidx.compose.ui.graphics.Color(0xFF90CAF9), // Light blue accent
            onTertiary = androidx.compose.ui.graphics.Color(0xFF000000),
            tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF1A3D5C),
            onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFE3F2FD),
            background = androidx.compose.ui.graphics.Color(0xFF0A1628), // Very dark navy background
            onBackground = androidx.compose.ui.graphics.Color(0xFFE8F4F8),
            surface = androidx.compose.ui.graphics.Color(0xFF121F35), // Dark navy surface
            onSurface = androidx.compose.ui.graphics.Color(0xFFE8F4F8),
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1E3A5F), // Navy variant
            onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC5D5E3),
            error = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
            onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            errorContainer = androidx.compose.ui.graphics.Color(0xFF8B0000),
            onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFE0E0)
        )
    } else {
        // Light theme: Navy blue base with light backgrounds
        lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF1E3A5F), // Navy blue primary
            onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            primaryContainer = androidx.compose.ui.graphics.Color(0xFFD6E8F5), // Light blue container
            onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF0A1628),
            secondary = androidx.compose.ui.graphics.Color(0xFF2C4A6B), // Darker navy for secondary
            onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            secondaryContainer = androidx.compose.ui.graphics.Color(0xFFE3F2FD), // Very light blue
            onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF0A1628),
            tertiary = androidx.compose.ui.graphics.Color(0xFF4A90E2), // Bright blue accent
            onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFD6E8F5),
            onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF0A1628),
            background = androidx.compose.ui.graphics.Color(0xFFF5F9FC), // Very light blue-gray background
            onBackground = androidx.compose.ui.graphics.Color(0xFF0A1628),
            surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF), // White surface
            onSurface = androidx.compose.ui.graphics.Color(0xFF0A1628),
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE3F2FD), // Light blue variant
            onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF1E3A5F),
            error = androidx.compose.ui.graphics.Color(0xFFC62828),
            onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            errorContainer = androidx.compose.ui.graphics.Color(0xFFFFE0E0),
            onErrorContainer = androidx.compose.ui.graphics.Color(0xFF8B0000)
        )
    }
}

@Composable
fun isDarkTheme(themeMode: ThemeMode): Boolean {
    return when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
}

fun setThemeMode(mode: ThemeMode) {
    ThemeState.setThemeMode(mode)
}
