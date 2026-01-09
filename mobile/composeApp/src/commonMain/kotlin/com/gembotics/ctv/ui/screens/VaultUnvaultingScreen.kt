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
import com.gembotics.ctv.models.UnvaultingRequest
import com.gembotics.ctv.ui.components.AppBottomBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultUnvaultingScreen(
    apiBaseUrl: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var vaultContext by remember { mutableStateOf("") }
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
                title = { Text("Unvault Funds") },
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
                value = vaultContext,
                onValueChange = { vaultContext = it },
                label = { Text("Vault Context (JSON from Vaulting)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste the Vault Context JSON here...") },
                minLines = 5
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = txid,
                onValueChange = { txid = it },
                label = { Text("Transaction ID (TXID)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Funding transaction ID") }
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
                            val request = UnvaultingRequest(
                                vault = vaultContext,
                                txid = txid,
                                vout = vout.toIntOrNull() ?: 0
                            )
                            val response = api.vaultUnvaulting(request)
                            result = """
                                Unvault Redeem Script:
                                ${response.script}
                                
                                Unvaulting Transaction:
                                ${response.tx}
                                
                                Unvaulting TXID:
                                ${response.txid}
                                
                                Updated Vault Context:
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
                enabled = !isLoading && vaultContext.isNotBlank() && txid.isNotBlank() && vout.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Create Unvaulting Transaction")
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
