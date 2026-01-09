package com.gembotics.ctv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gembotics.ctv.api.createCtvApi
import com.gembotics.ctv.models.VaultVerificationRequest
import com.gembotics.ctv.models.VaultVerificationResponse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultVerificationScreen(
    apiBaseUrl: String,
    onBack: () -> Unit
) {
    var vaultContext by remember { mutableStateOf("") }
    var txHex by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<VaultVerificationResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val api = remember { createCtvApi(apiBaseUrl) }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Vault Transaction") },
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
                value = vaultContext,
                onValueChange = { vaultContext = it },
                label = { Text("Vault Context (JSON)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste the Vault Context JSON here...") },
                minLines = 5
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = txHex,
                onValueChange = { txHex = it },
                label = { Text("Transaction Hex") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste the hex-encoded transaction to verify") },
                minLines = 5
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    isLoading = true
                    error = null
                    result = null
                    coroutineScope.launch {
                        try {
                            val request = VaultVerificationRequest(
                                vault = vaultContext,
                                tx = txHex
                            )
                            val response = api.vaultVerification(request)
                            result = response
                            isLoading = false
                        } catch (e: Exception) {
                            error = "Error: ${e.message}"
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && vaultContext.isNotBlank() && txHex.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Verify Transaction")
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
            
            result?.let { verificationResult ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (verificationResult.valid) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Verification Result",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (verificationResult.valid) {
                                Text(
                                    "✓ Valid",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            } else {
                                Text(
                                    "✗ Invalid",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "Transaction Type: ${verificationResult.transactionType}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            verificationResult.message,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
