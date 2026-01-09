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
        darkColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFFFF7043),
            secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6),
            tertiary = androidx.compose.ui.graphics.Color(0xFF3700B3)
        )
    } else {
        lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFFF4511E),
            secondary = androidx.compose.ui.graphics.Color(0xFF03DAC6),
            tertiary = androidx.compose.ui.graphics.Color(0xFF3700B3)
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
