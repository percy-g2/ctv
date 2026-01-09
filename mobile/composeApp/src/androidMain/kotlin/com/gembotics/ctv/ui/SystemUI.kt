package com.gembotics.ctv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun SetStatusBarAppearance(useDarkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            window?.let {
                val windowInsetsController = WindowCompat.getInsetsController(it, view)
                windowInsetsController.isAppearanceLightStatusBars = !useDarkTheme
            }
        }
    }
}
