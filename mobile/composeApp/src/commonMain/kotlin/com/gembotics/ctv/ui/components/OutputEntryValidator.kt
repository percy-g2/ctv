package com.gembotics.ctv.ui.components

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

object OutputEntryValidator {
    fun validateAddress(address: String, network: String): ValidationResult {
        if (address.isBlank()) {
            return ValidationResult(false, "Address cannot be blank")
        }
        
        // Basic format validation based on network
        when (network) {
            "bitcoin" -> {
                if (!address.startsWith("bc1") && 
                    !address.startsWith("1") && 
                    !address.startsWith("3")) {
                    return ValidationResult(
                        false, 
                        "Mainnet addresses must start with bc1, 1, or 3"
                    )
                }
            }
            "testnet" -> {
                if (!address.startsWith("tb1") && 
                    !address.startsWith("m") && 
                    !address.startsWith("n") && 
                    !address.startsWith("2")) {
                    return ValidationResult(
                        false, 
                        "Testnet addresses must start with tb1, m, n, or 2"
                    )
                }
            }
        }
        
        return ValidationResult(true)
    }
    
    fun validateAmount(amount: String): ValidationResult {
        if (amount.isBlank()) {
            return ValidationResult(false, "Amount cannot be blank")
        }
        
        return try {
            val amountValue = amount.toLong()
            if (amountValue <= 0) {
                ValidationResult(false, "Amount must be greater than 0")
            } else {
                ValidationResult(true)
            }
        } catch (e: NumberFormatException) {
            ValidationResult(false, "Amount must be a valid number")
        }
    }
    
    fun validateData(data: String): ValidationResult {
        if (data.isBlank()) {
            return ValidationResult(true) // Data is optional
        }
        
        // Validate hex format if provided
        if (data.length % 2 != 0) {
            return ValidationResult(false, "Hex data must have even length")
        }
        
        if (!data.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            return ValidationResult(false, "Data must be valid hex")
        }
        
        return ValidationResult(true)
    }
    
    fun validateEntry(entry: OutputEntry, network: String): ValidationResult {
        val addressResult = validateAddress(entry.address, network)
        if (!addressResult.isValid) {
            return addressResult
        }
        
        val amountResult = validateAmount(entry.amount)
        if (!amountResult.isValid) {
            return amountResult
        }
        
        val dataResult = validateData(entry.data)
        if (!dataResult.isValid) {
            return dataResult
        }
        
        return ValidationResult(true)
    }
}
