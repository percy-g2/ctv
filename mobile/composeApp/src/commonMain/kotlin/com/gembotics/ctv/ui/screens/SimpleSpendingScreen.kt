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
import com.gembotics.ctv.models.SpendingRequest
import com.gembotics.ctv.ui.components.AppBottomBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSpendingScreen(
    apiBaseUrl: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var ctvContext by remember { mutableStateOf("") }
    var txid by remember { mutableStateOf("") }
    var vout by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val api = remember { createCtvApi(apiBaseUrl) }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Spending Transaction") },
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
                value = ctvContext,
                onValueChange = { ctvContext = it },
                label = { Text("CTV Context (JSON from Locking)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste the CTV Context JSON here...") },
                minLines = 5
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = txid,
                onValueChange = { txid = it },
                label = { Text("Transaction ID (TXID)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Previous transaction ID") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = vout,
                onValueChange = { vout = it },
                label = { Text("Output Index (Vout)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("0") }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    isLoading = true
                    error = null
                    result = null
                    coroutineScope.launch {
                        try {
                            val request = SpendingRequest(
                                ctv = ctvContext,
                                txid = txid,
                                vout = vout.toIntOrNull() ?: 0
                            )
                            val response = api.simpleSpending(request)
                            result = "Spending Transactions:\n\n" + 
                                    response.txs.mapIndexed { index, tx -> 
                                        "Transaction ${index + 1}:\n$tx"
                                    }.joinToString("\n\n")
                            isLoading = false
                        } catch (e: Exception) {
                            error = "Error: ${e.message}"
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && ctvContext.isNotBlank() && txid.isNotBlank() && vout.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Create Spending Transaction")
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
