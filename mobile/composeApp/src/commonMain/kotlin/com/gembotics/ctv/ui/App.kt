package com.gembotics.ctv.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.gembotics.ctv.SERVER_PORT
import com.gembotics.ctv.ui.navigation.Screen
import com.gembotics.ctv.ui.screens.*
import com.gembotics.ctv.ui.theme.ThemeState
import com.gembotics.ctv.ui.theme.getColorScheme
import com.gembotics.ctv.ui.theme.isDarkTheme

@Composable
fun App() {
    val themeMode by ThemeState.themeMode
    
    val colorScheme = getColorScheme(themeMode)
    val useDarkTheme = isDarkTheme(themeMode)
    
    // Set status bar appearance based on app theme
    SetStatusBarAppearance(useDarkTheme)
    
    MaterialTheme(colorScheme = colorScheme) {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
        val apiBaseUrl = remember { "http://10.0.2.2:$SERVER_PORT" } // Android emulator localhost
        
        when (currentScreen) {
            is Screen.Home -> {
                HomeScreen(
                    onNavigate = { currentScreen = it },
                    onSettingsClick = { currentScreen = Screen.Settings }
                )
            }
            is Screen.SimpleCtv -> {
                SimpleCtvScreen(
                    onNavigate = { currentScreen = it },
                    onBack = { currentScreen = Screen.Home },
                    onSettingsClick = { currentScreen = Screen.Settings }
                )
            }
            is Screen.SimpleLocking -> {
                SimpleLockingScreen(
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = Screen.SimpleCtv },
                    onSettingsClick = { currentScreen = Screen.Settings }
                )
            }
            is Screen.SimpleSpending -> {
                SimpleSpendingScreen(
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = Screen.SimpleCtv },
                    onSettingsClick = { currentScreen = Screen.Settings }
                )
            }
            is Screen.Vaults -> {
                VaultsScreen(
                    onNavigate = { currentScreen = it },
                    onBack = { currentScreen = Screen.Home },
                    onSettingsClick = { currentScreen = Screen.Settings }
                )
            }
            is Screen.VaultVaulting -> {
                VaultVaultingScreen(
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = Screen.Vaults },
                    onSettingsClick = { currentScreen = Screen.Settings }
                )
            }
            is Screen.VaultUnvaulting -> {
                VaultUnvaultingScreen(
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = Screen.Vaults },
                    onSettingsClick = { currentScreen = Screen.Settings }
                )
            }
            is Screen.VaultSpending -> {
                VaultSpendingScreen(
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = Screen.Vaults },
                    onSettingsClick = { currentScreen = Screen.Settings }
                )
            }
            is Screen.VaultVerification -> {
                VaultVerificationScreen(
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = Screen.Vaults },
                    onSettingsClick = { currentScreen = Screen.Settings }
                )
            }
            is Screen.TestCases -> {
                TestCasesScreen(
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = Screen.Home },
                    onSettingsClick = { currentScreen = Screen.Settings }
                )
            }
            is Screen.Settings -> {
                SettingsScreen(
                    onBack = { 
                        // Navigate back to previous screen or Home
                        currentScreen = Screen.Home 
                    }
                )
            }
        }
    }
}
