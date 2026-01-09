package com.gembotics.ctv.routes

import com.gembotics.ctv.ctv.NetworkUtils
import com.gembotics.ctv.ctv.VaultHelper
import com.gembotics.ctv.models.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bitcoinj.base.Address as BitcoinAddress
import org.bitcoinj.base.SegwitAddress
import org.bitcoinj.base.Coin
import org.bitcoinj.base.Sha256Hash
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.base.exceptions.AddressFormatException
import kotlinx.serialization.json.*
import java.math.BigInteger

/**
 * Vault routes with bitcoinj implementation
 */
fun Route.vaultRoutes() {
    route("/vaults") {
        get {
            call.respond(mapOf("message" to "CTV Vaults Playground"))
        }
        
        post("/vaulting") {
            try {
                val request = call.receive<VaultingRequest>()
                val params = NetworkUtils.getNetworkParams(request.network)
                
                val amount = Coin.valueOf(request.amount.toLong())
                val coldAddress = BitcoinAddress.fromString(params, request.coldAddress)
                val hotAddress = BitcoinAddress.fromString(params, request.hotAddress)
                val blockDelay = request.blockDelay
                
                // Create vault address
                val vaultAddress = VaultHelper.createVaultAddress(
                    coldAddress,
                    hotAddress,
                    amount,
                    blockDelay,
                    params
                )
                
                // Create vault JSON
                val vaultJson = buildJsonObject {
                    put("hotAddress", request.hotAddress)
                    put("coldAddress", request.coldAddress)
                    put("amount", request.amount)
                    put("network", request.network)
                    put("blockDelay", blockDelay)
                    put("taproot", request.taproot)
                }
                
                val response = VaultingResponse(
                    vault = vaultJson.toString(),
                    address = vaultAddress.toString()
                )
                
                call.respond(response)
            } catch (e: Exception) {
                call.respond(
                    io.ktor.http.HttpStatusCode.BadRequest,
                    mapOf("error" to (e.message ?: "Invalid request"))
                )
            }
        }
        
        post("/unvaulting") {
            try {
                val request = call.receive<UnvaultingRequest>()
                
                // Parse vault JSON - handle both direct JSON object and JSON-encoded string
                val vaultJsonElement = Json.parseToJsonElement(request.vault)
                val vaultJson = when {
                    vaultJsonElement is JsonObject -> vaultJsonElement
                    vaultJsonElement is JsonPrimitive && vaultJsonElement.isString -> {
                        // If it's a JSON-encoded string, parse its content
                        Json.parseToJsonElement(vaultJsonElement.content).jsonObject
                    }
                    else -> throw IllegalArgumentException("Invalid vault JSON format: expected JSON object or JSON string")
                }
                val network = vaultJson["network"]?.jsonPrimitive?.content ?: "testnet"
                val params = NetworkUtils.getNetworkParams(network)
                
                val coldAddress = BitcoinAddress.fromString(
                    params,
                    vaultJson["coldAddress"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("Missing coldAddress")
                )
                val hotAddress = BitcoinAddress.fromString(
                    params,
                    vaultJson["hotAddress"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("Missing hotAddress")
                )
                val amount = Coin.valueOf(
                    vaultJson["amount"]?.jsonPrimitive?.content?.toLong()
                        ?: throw IllegalArgumentException("Missing amount")
                )
                val blockDelay = vaultJson["blockDelay"]?.jsonPrimitive?.int
                    ?: throw IllegalArgumentException("Missing blockDelay")
                
                // Create CTV hashes
                val coldCtvHash = try {
                    VaultHelper.createColdCtv(coldAddress, amount, params)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Failed to create cold CTV hash: ${e.message}", e)
                }
                val hotCtvHash = try {
                    VaultHelper.createHotCtv(hotAddress, amount, blockDelay, params)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Failed to create hot CTV hash: ${e.message}", e)
                }
                
                // Create unvault redeem script bytes (skip Script object creation to avoid bitcoinj bug)
                // Note: We don't actually need the Script object, just the bytes for address creation
                val unvaultRedeemScriptBytes = try {
                    VaultHelper.createUnvaultRedeemScriptBytes(
                        coldCtvHash,
                        hotCtvHash,
                        blockDelay
                    )
                } catch (e: Exception) {
                    throw IllegalArgumentException("Failed to create unvault redeem script bytes: ${e.message}", e)
                }
                
                // Create unvault address
                val unvaultAddress = try {
                    VaultHelper.createUnvaultAddress(
                        coldCtvHash,
                        hotCtvHash,
                        blockDelay,
                        params
                    )
                } catch (e: Exception) {
                    throw IllegalArgumentException("Failed to create unvault address: ${e.message}. " +
                            "Cold hash size: ${coldCtvHash.size}, Hot hash size: ${hotCtvHash.size}", e)
                }
                
                // Create vault CTV hash
                val vaultCtvHash = VaultHelper.createVaultCtv(unvaultAddress, amount, params)
                
                // Parse txid and vout
                val prevTxId = try {
                    // Ensure txid is valid hex and exactly 64 characters (32 bytes)
                    val txidHex = request.txid.trim().replace(" ", "")
                    if (txidHex.length != 64) {
                        throw IllegalArgumentException("Transaction ID must be exactly 64 hex characters (32 bytes), got ${txidHex.length}")
                    }
                    Sha256Hash.wrap(txidHex)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid transaction ID '${request.txid}': ${e.message}")
                }
                val prevVout = request.vout
                
                // Create unvault transaction
                val unvaultTx = VaultHelper.createUnvaultTransaction(
                    vaultCtvHash,
                    prevTxId,
                    prevVout,
                    unvaultAddress,
                    amount,
                    params
                )
                
                val txHex = unvaultTx.bitcoinSerialize().toHex()
                val txid = unvaultTx.txId.toString()
                
                // Get redeem script hex for response (create bytes version to avoid Script constructor bug)
                val redeemScriptBytes = VaultHelper.createUnvaultRedeemScriptBytes(
                    coldCtvHash,
                    hotCtvHash,
                    blockDelay
                )
                val redeemScriptHex = redeemScriptBytes.joinToString("") { "%02x".format(it) }
                
                val response = UnvaultingResponse(
                    vault = request.vault,
                    script = redeemScriptHex,
                    tx = txHex,
                    txid = txid
                )
                
                call.respond(response)
            } catch (e: Exception) {
                call.respond(
                    io.ktor.http.HttpStatusCode.BadRequest,
                    mapOf("error" to (e.message ?: "Invalid request"))
                )
            }
        }
        
        post("/spending") {
            try {
                val request = call.receive<VaultSpendingRequest>()
                
                // Parse vault JSON - handle both direct JSON object and JSON-encoded string
                val vaultJsonElement = Json.parseToJsonElement(request.vault)
                val vaultJson = when {
                    vaultJsonElement is JsonObject -> vaultJsonElement
                    vaultJsonElement is JsonPrimitive && vaultJsonElement.isString -> {
                        // If it's a JSON-encoded string, parse its content
                        Json.parseToJsonElement(vaultJsonElement.content).jsonObject
                    }
                    else -> throw IllegalArgumentException("Invalid vault JSON format: expected JSON object or JSON string")
                }
                val network = vaultJson["network"]?.jsonPrimitive?.content ?: "testnet"
                val params = NetworkUtils.getNetworkParams(network)
                
                val coldAddress = BitcoinAddress.fromString(
                    params,
                    vaultJson["coldAddress"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("Missing coldAddress")
                )
                val hotAddress = BitcoinAddress.fromString(
                    params,
                    vaultJson["hotAddress"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("Missing hotAddress")
                )
                val amount = Coin.valueOf(
                    vaultJson["amount"]?.jsonPrimitive?.content?.toLong()
                        ?: throw IllegalArgumentException("Missing amount")
                )
                val blockDelay = vaultJson["blockDelay"]?.jsonPrimitive?.int
                    ?: throw IllegalArgumentException("Missing blockDelay")
                
                // Create CTV hashes
                val coldCtvHash = VaultHelper.createColdCtv(coldAddress, amount, params)
                val hotCtvHash = VaultHelper.createHotCtv(hotAddress, amount, blockDelay, params)
                
                // Create unvault redeem script
                val unvaultRedeemScript = VaultHelper.createUnvaultRedeemScript(
                    coldCtvHash,
                    hotCtvHash,
                    blockDelay,
                    params
                )
                
                // Parse txid (this is the unvault transaction ID)
                val prevTxId = Sha256Hash.wrap(request.txid)
                val prevVout = 0 // Unvault transaction has output at index 0
                
                // Create cold spend transaction
                val coldTx = VaultHelper.createColdSpendTransaction(
                    unvaultRedeemScript,
                    prevTxId,
                    prevVout,
                    coldAddress,
                    amount,
                    params
                )
                
                // Create hot spend transaction
                val hotTx = VaultHelper.createHotSpendTransaction(
                    unvaultRedeemScript,
                    prevTxId,
                    prevVout,
                    hotAddress,
                    amount,
                    blockDelay,
                    params
                )
                
                val response = VaultSpendingResponse(
                    coldTx = coldTx.bitcoinSerialize().toHex(),
                    hotTx = hotTx.bitcoinSerialize().toHex()
                )
                
                call.respond(response)
            } catch (e: Exception) {
                call.respond(
                    io.ktor.http.HttpStatusCode.BadRequest,
                    mapOf("error" to (e.message ?: "Invalid request"))
                )
            }
        }
        
        post("/verification") {
            try {
                val request = call.receive<VaultVerificationRequest>()
                
                // Parse vault JSON - handle both direct JSON object and JSON-encoded string
                val vaultJsonElement = Json.parseToJsonElement(request.vault)
                val vaultJson = when {
                    vaultJsonElement is JsonObject -> vaultJsonElement
                    vaultJsonElement is JsonPrimitive && vaultJsonElement.isString -> {
                        // If it's a JSON-encoded string, parse its content
                        Json.parseToJsonElement(vaultJsonElement.content).jsonObject
                    }
                    else -> throw IllegalArgumentException("Invalid vault JSON format: expected JSON object or JSON string")
                }
                val network = vaultJson["network"]?.jsonPrimitive?.content ?: "testnet"
                val params = NetworkUtils.getNetworkParams(network)
                
                val coldAddressStr = vaultJson["coldAddress"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Missing coldAddress")
                val hotAddressStr = vaultJson["hotAddress"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("Missing hotAddress")
                
                // Helper function to parse address (supports both legacy and bech32)
                fun parseAddress(addressStr: String, addressType: String): BitcoinAddress {
                    return try {
                        // Try bech32 (SegWit) addresses first
                        if (addressStr.startsWith("bc1") || addressStr.startsWith("tb1")) {
                            // For bech32, always use null params to let bitcoinj derive network from address
                            // This is the recommended approach per bitcoinj API
                            SegwitAddress.fromBech32(null, addressStr.trim()) as BitcoinAddress
                        } else {
                            // Try legacy addresses
                            BitcoinAddress.fromString(params, addressStr.trim())
                        }
                    } catch (e: AddressFormatException) {
                        // Specific handling for address format errors
                        val errorMsg = e.message ?: "Invalid address format"
                        throw IllegalArgumentException("Invalid $addressType address '$addressStr': $errorMsg. " +
                                "For testnet bech32 addresses, ensure they start with 'tb1' and have a valid checksum.")
                    } catch (e: Exception) {
                        val errorMsg = if (e.message == addressStr || e.message.isNullOrBlank()) {
                            "Address format not recognized. For testnet, use tb1... (bech32) or m..., n..., 2... (legacy) addresses. Exception: ${e.javaClass.simpleName}"
                        } else {
                            e.message ?: e.javaClass.simpleName
                        }
                        throw IllegalArgumentException("Invalid $addressType address '$addressStr': $errorMsg")
                    }
                }
                
                val coldAddress = parseAddress(coldAddressStr, "cold")
                val hotAddress = parseAddress(hotAddressStr, "hot")
                
                val amount = Coin.valueOf(
                    vaultJson["amount"]?.jsonPrimitive?.content?.toLong()
                        ?: throw IllegalArgumentException("Missing amount")
                )
                val blockDelay = vaultJson["blockDelay"]?.jsonPrimitive?.int
                    ?: throw IllegalArgumentException("Missing blockDelay")
                
                // Verify transaction
                val result = VaultHelper.verifyTransaction(
                    request.tx,
                    coldAddress,
                    hotAddress,
                    amount,
                    blockDelay,
                    params
                )
                
                val response = VaultVerificationResponse(
                    transactionType = result.transactionType,
                    valid = result.valid,
                    message = result.message
                )
                
                call.respond(response)
            } catch (e: Exception) {
                call.respond(
                    io.ktor.http.HttpStatusCode.BadRequest,
                    mapOf("error" to (e.message ?: "Invalid request"))
                )
            }
        }
    }
}

// Extension function
private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
