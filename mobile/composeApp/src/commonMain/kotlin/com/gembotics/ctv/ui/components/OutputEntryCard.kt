package com.gembotics.ctv.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OutputEntryCard(
    entry: OutputEntry,
    network: String,
    index: Int,
    onAddressChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDataChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val addressValidation = remember(entry.address, network) {
        OutputEntryValidator.validateAddress(entry.address, network)
    }
    val amountValidation = remember(entry.amount) {
        OutputEntryValidator.validateAmount(entry.amount)
    }
    val dataValidation = remember(entry.data) {
        OutputEntryValidator.validateData(entry.data)
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    "Output ${index + 1}",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onRemove) {
                    Text("✕", style = MaterialTheme.typography.titleLarge)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = entry.address,
                onValueChange = onAddressChange,
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(when(network) {
                        "bitcoin" -> "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh"
                        "testnet" -> "tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx"
                        else -> "Enter address"
                    })
                },
                isError = !addressValidation.isValid && entry.address.isNotBlank(),
                supportingText = {
                    if (!addressValidation.isValid && entry.address.isNotBlank()) {
                        Text(addressValidation.errorMessage ?: "")
                    }
                },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = entry.amount,
                onValueChange = onAmountChange,
                label = { Text("Amount (satoshis)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("100000") },
                isError = !amountValidation.isValid && entry.amount.isNotBlank(),
                supportingText = {
                    if (!amountValidation.isValid && entry.amount.isNotBlank()) {
                        Text(amountValidation.errorMessage ?: "")
                    }
                },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = entry.data,
                onValueChange = onDataChange,
                label = { Text("Data (hex, optional)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Leave empty or enter hex data") },
                isError = !dataValidation.isValid && entry.data.isNotBlank(),
                supportingText = {
                    if (!dataValidation.isValid && entry.data.isNotBlank()) {
                        Text(dataValidation.errorMessage ?: "")
                    } else {
                        Text("Optional: Hex data for OP_RETURN output")
                    }
                },
                singleLine = true
            )
        }
    }
}
