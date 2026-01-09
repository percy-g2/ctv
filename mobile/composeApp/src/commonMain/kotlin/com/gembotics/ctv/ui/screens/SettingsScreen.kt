package com.gembotics.ctv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gembotics.ctv.ui.theme.ThemeMode
import com.gembotics.ctv.ui.theme.ThemeState
import com.gembotics.ctv.ui.theme.setThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    var currentThemeMode by ThemeState.themeMode
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleLarge
            )
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOption(
                        label = "Light",
                        selected = currentThemeMode == ThemeMode.Light,
                        onClick = {
                            currentThemeMode = ThemeMode.Light
                            setThemeMode(ThemeMode.Light)
                        }
                    )
                    
                    Divider()
                    
                    ThemeOption(
                        label = "Dark",
                        selected = currentThemeMode == ThemeMode.Dark,
                        onClick = {
                            currentThemeMode = ThemeMode.Dark
                            setThemeMode(ThemeMode.Dark)
                        }
                    )
                    
                    Divider()
                    
                    ThemeOption(
                        label = "System",
                        selected = currentThemeMode == ThemeMode.System,
                        onClick = {
                            currentThemeMode = ThemeMode.System
                            setThemeMode(ThemeMode.System)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}
