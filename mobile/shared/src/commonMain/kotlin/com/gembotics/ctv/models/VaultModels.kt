package com.gembotics.ctv.models

import kotlinx.serialization.Serializable

// Vault Models

@Serializable
data class VaultingRequest(
    val amount: String, // Amount in satoshis as string
    val coldAddress: String,
    val hotAddress: String,
    val blockDelay: Int,
    val network: String, // "bitcoin", "testnet", "signet", "regtest"
    val taproot: Boolean = false
)

@Serializable
data class VaultingResponse(
    val vault: String, // JSON string of vault
    val address: String
)

@Serializable
data class UnvaultingRequest(
    val vault: String, // JSON string of vault
    val txid: String,
    val vout: Int
)

@Serializable
data class UnvaultingResponse(
    val vault: String, // JSON string of vault
    val script: String,
    val tx: String, // Hex-encoded transaction
    val txid: String
)

@Serializable
data class VaultSpendingRequest(
    val vault: String, // JSON string of vault
    val txid: String
)

@Serializable
data class VaultSpendingResponse(
    val coldTx: String, // Hex-encoded cold transaction
    val hotTx: String // Hex-encoded hot transaction
)
