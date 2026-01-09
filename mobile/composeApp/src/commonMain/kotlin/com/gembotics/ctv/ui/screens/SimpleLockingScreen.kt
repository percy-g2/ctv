package com.gembotics.ctv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gembotics.ctv.api.createCtvApi
import com.gembotics.ctv.models.LockingRequest
import com.gembotics.ctv.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleLockingScreen(
    apiBaseUrl: String,
    onBack: () -> Unit
) {
    var outputs by remember { mutableStateOf("") }
    var network by remember { mutableStateOf("testnet") }
    var congestion by remember { mutableStateOf(false) }
    var taproot by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val api = remember { createCtvApi(apiBaseUrl) }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Locking Script") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
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
                value = outputs,
                onValueChange = { outputs = it },
                label = { Text("Outputs (address:amount:data, one per line)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    Text(when(network) {
                        "bitcoin" -> "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh:100000:\nbc1q...:50000:"
                        "testnet" -> "tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx:100000:\nmzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef:50000:"
                        else -> "address:amount:data\naddress2:amount2:data2"
                    })
                },
                minLines = 3
            )
            
            Text(
                text = when(network) {
                    "bitcoin" -> "Format: address:amount:data (one per line)\nMainnet addresses: bc1... (bech32) or 1..., 3... (legacy)\nExample: bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh:100000:"
                    "testnet" -> "Format: address:amount:data (one per line)\nTestnet addresses: tb1... (bech32) or m..., n..., 2... (legacy)\nExample: tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx:100000:"
                    else -> "Format: address:amount:data (one per line)\nExample: address:100000:"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
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
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("bitcoin", "testnet", "signet", "regtest").forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                network = item
                                expanded = false
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
                        checked = congestion,
                        onCheckedChange = { congestion = it }
                    )
                    Text("Congestion Control")
                }
                
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
                            val request = LockingRequest(
                                outputs = outputs,
                                network = network,
                                congestion = congestion,
                                taproot = taproot
                            )
                            val response = api.simpleLocking(request)
                            result = """
                                CTV Hash: ${response.ctvHash}
                                Locking Script: ${response.lockingScript}
                                Locking Hex: ${response.lockingHex}
                                Address: ${response.address}
                                CTV Context: ${response.ctv}
                            """.trimIndent()
                            isLoading = false
                        } catch (e: Exception) {
                            val errorMsg = e.message ?: "Unknown error"
                            val helpfulMsg = when {
                                errorMsg.contains("Invalid address") && network == "testnet" -> 
                                    "$errorMsg\n\nTip: Use testnet addresses:\n- tb1... (bech32)\n- m..., n..., 2... (legacy)\nExample: tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx:100000:"
                                errorMsg.contains("Invalid address") && network == "bitcoin" -> 
                                    "$errorMsg\n\nTip: Use mainnet addresses:\n- bc1... (bech32)\n- 1..., 3... (legacy)"
                                else -> errorMsg
                            }
                            error = helpfulMsg
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && outputs.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Create Locking Script")
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
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
