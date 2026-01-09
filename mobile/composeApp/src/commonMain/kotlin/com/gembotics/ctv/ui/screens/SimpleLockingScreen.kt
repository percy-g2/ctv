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
import com.gembotics.ctv.models.LockingRequest
import com.gembotics.ctv.ui.components.AppBottomBar
import com.gembotics.ctv.ui.components.OutputEntry
import com.gembotics.ctv.ui.components.OutputEntryCard
import com.gembotics.ctv.ui.components.OutputEntryValidator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleLockingScreen(
    apiBaseUrl: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var outputEntries by remember { 
        mutableStateOf(listOf(
            OutputEntry(address = "tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx", amount = "100000", data = ""),
            OutputEntry(address = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef", amount = "50000", data = "")
        )) 
    }
    var network by remember { mutableStateOf("testnet") }
    var congestion by remember { mutableStateOf(false) }
    var taproot by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val api = remember { createCtvApi(apiBaseUrl) }
    val coroutineScope = rememberCoroutineScope()
    
    // Convert entries to output string format
    fun entriesToOutputString(): String {
        return outputEntries
            .filter { it.address.isNotBlank() && it.amount.isNotBlank() }
            .joinToString("\n") { it.toOutputString() }
    }
    
    // Validate all entries
    val allEntriesValid = remember(outputEntries, network) {
        outputEntries.all { entry ->
            entry.address.isBlank() && entry.amount.isBlank() || 
            OutputEntryValidator.validateEntry(entry, network).isValid
        }
    }
    
    val hasValidEntries = remember(outputEntries) {
        outputEntries.any { it.address.isNotBlank() && it.amount.isNotBlank() }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Locking Script") },
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
            Text(
                "Outputs",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = when(network) {
                    "bitcoin" -> "Mainnet addresses: bc1... (bech32) or 1..., 3... (legacy)"
                    "testnet" -> "Testnet addresses: tb1... (bech32) or m..., n..., 2... (legacy)"
                    else -> "Enter address and amount for each output"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            outputEntries.forEachIndexed { index, entry ->
                OutputEntryCard(
                    entry = entry,
                    network = network,
                    index = index,
                    onAddressChange = { newAddress ->
                        outputEntries = outputEntries.toMutableList().apply {
                            this[index] = this[index].copy(address = newAddress)
                        }
                    },
                    onAmountChange = { newAmount ->
                        outputEntries = outputEntries.toMutableList().apply {
                            this[index] = this[index].copy(amount = newAmount)
                        }
                    },
                    onDataChange = { newData ->
                        outputEntries = outputEntries.toMutableList().apply {
                            this[index] = this[index].copy(data = newData)
                        }
                    },
                    onRemove = {
                        if (outputEntries.size > 1) {
                            outputEntries = outputEntries.toMutableList().apply {
                                removeAt(index)
                            }
                        }
                    },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            
            Button(
                onClick = {
                    outputEntries = outputEntries + OutputEntry()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add Output")
            }
            
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
                            val outputsString = entriesToOutputString()
                            if (outputsString.isBlank()) {
                                error = "At least one output with address and amount is required"
                                isLoading = false
                                return@launch
                            }
                            
                            val request = LockingRequest(
                                outputs = outputsString,
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
                enabled = !isLoading && hasValidEntries && allEntriesValid
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
