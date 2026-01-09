package com.gembotics.ctv.ui

import androidx.compose.runtime.Composable

// No-op for non-Android platforms
@Composable
expect fun SetStatusBarAppearance(useDarkTheme: Boolean)
