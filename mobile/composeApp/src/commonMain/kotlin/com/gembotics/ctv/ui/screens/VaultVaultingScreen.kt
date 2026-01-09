package com.gembotics.ctv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gembotics.ctv.api.createCtvApi
import com.gembotics.ctv.models.VaultingRequest
import com.gembotics.ctv.ui.components.AppBottomBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultVaultingScreen(
    apiBaseUrl: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var coldAddress by remember { mutableStateOf("") }
    var hotAddress by remember { mutableStateOf("") }
    var blockDelay by remember { mutableStateOf("") }
    var network by remember { mutableStateOf("testnet") }
    var taproot by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val api = remember { createCtvApi(apiBaseUrl) }
    val coroutineScope = rememberCoroutineScope()
    
    var networkExpanded by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Vault") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (satoshis)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("1000000") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = coldAddress,
                onValueChange = { coldAddress = it },
                label = { Text("Cold Address") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cold storage address") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = hotAddress,
                onValueChange = { hotAddress = it },
                label = { Text("Hot Address") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Hot wallet address") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = blockDelay,
                onValueChange = { blockDelay = it },
                label = { Text("Block Delay") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("144 (1 day)") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ExposedDropdownMenuBox(
                expanded = networkExpanded,
                onExpandedChange = { networkExpanded = !networkExpanded }
            ) {
                OutlinedTextField(
                    value = network,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Network") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = networkExpanded,
                    onDismissRequest = { networkExpanded = false }
                ) {
                    listOf("bitcoin", "testnet", "signet", "regtest").forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                network = item
                                networkExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(
                        checked = taproot,
                        onCheckedChange = { taproot = it }
                    )
                    Text("Taproot")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    isLoading = true
                    error = null
                    result = null
                    coroutineScope.launch {
                        try {
                            val request = VaultingRequest(
                                amount = amount,
                                coldAddress = coldAddress,
                                hotAddress = hotAddress,
                                blockDelay = blockDelay.toIntOrNull() ?: 144,
                                network = network,
                                taproot = taproot
                            )
                            val response = api.vaultVaulting(request)
                            result = """
                                Vault Address: ${response.address}
                                
                                Vault Context:
                                ${response.vault}
                            """.trimIndent()
                            isLoading = false
                        } catch (e: Exception) {
                            error = "Error: ${e.message}"
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && amount.isNotBlank() && coldAddress.isNotBlank() && hotAddress.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Create Vault")
                }
            }
            
            error?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            result?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Result:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
