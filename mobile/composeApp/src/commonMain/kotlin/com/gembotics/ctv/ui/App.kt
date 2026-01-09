package com.gembotics.ctv.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.gembotics.ctv.SERVER_PORT
import com.gembotics.ctv.ui.navigation.Screen
import com.gembotics.ctv.ui.screens.*

@Composable
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
        val apiBaseUrl = remember { "http://10.0.2.2:$SERVER_PORT" } // Android emulator localhost
        
        when (currentScreen) {
            is Screen.Home -> {
                HomeScreen(
                    onNavigate = { currentScreen = it }
                )
            }
            is Screen.SimpleCtv -> {
                SimpleCtvScreen(
                    onNavigate = { currentScreen = it },
                    onBack = { currentScreen = Screen.Home }
                )
            }
            is Screen.SimpleLocking -> {
                SimpleLockingScreen(
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = Screen.SimpleCtv }
                )
            }
            is Screen.SimpleSpending -> {
                SimpleSpendingScreen(
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = Screen.SimpleCtv }
                )
            }
            is Screen.Vaults -> {
                VaultsScreen(
                    onNavigate = { currentScreen = it },
                    onBack = { currentScreen = Screen.Home }
                )
            }
            is Screen.VaultVaulting -> {
                VaultVaultingScreen(
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = Screen.Vaults }
                )
            }
            is Screen.VaultUnvaulting -> {
                VaultUnvaultingScreen(
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = Screen.Vaults }
                )
            }
            is Screen.VaultSpending -> {
                VaultSpendingScreen(
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = Screen.Vaults }
                )
            }
            is Screen.VaultVerification -> {
                VaultVerificationScreen(
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = Screen.Vaults }
                )
            }
            is Screen.TestCases -> {
                TestCasesScreen(
                    apiBaseUrl = apiBaseUrl,
                    onBack = { currentScreen = Screen.Home }
                )
            }
        }
    }
}
