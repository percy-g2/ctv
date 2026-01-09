package com.gembotics.ctv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gembotics.ctv.ui.components.AppBottomBar
import com.gembotics.ctv.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit,
    onSettingsClick: () -> Unit
) {
    Scaffold(
        bottomBar = {
            AppBottomBar(onSettingsClick = onSettingsClick)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "CTV Playground",
                style = MaterialTheme.typography.headlineLarge
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { onNavigate(Screen.SimpleCtv) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simple CTV")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onNavigate(Screen.Vaults) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CTV Vaults")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onNavigate(Screen.TestCases) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Run Test Cases")
            }
        }
    }
}
