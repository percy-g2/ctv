package com.gembotics.ctv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gembotics.ctv.ui.components.AppBottomBar
import com.gembotics.ctv.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleCtvScreen(
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Simple CTV") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "back"
                        )
                    }
                }
            )
        },
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
            Button(
                onClick = { onNavigate(Screen.SimpleLocking) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Locking Script")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onNavigate(Screen.SimpleSpending) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Spending Transaction")
            }
        }
    }
}
