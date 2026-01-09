package com.gembotics.ctv.models

import kotlinx.serialization.Serializable

// Simple CTV Models

@Serializable
data class LockingRequest(
    val outputs: String,
    val network: String, // "bitcoin", "testnet", "signet", "regtest"
    val congestion: Boolean = false,
    val taproot: Boolean = false
)

@Serializable
data class LockingResponse(
    val ctvHash: String,
    val lockingScript: String,
    val lockingHex: String,
    val address: String,
    val ctv: String // JSON string of CTV context
)

@Serializable
data class SpendingRequest(
    val ctv: String, // JSON string of CTV context
    val txid: String,
    val vout: Int
)

@Serializable
data class SpendingResponse(
    val txs: List<String> // List of hex-encoded transactions
)
