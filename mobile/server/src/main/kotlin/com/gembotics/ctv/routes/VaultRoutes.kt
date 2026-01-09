package com.gembotics.ctv.routes

import com.gembotics.ctv.ctv.NetworkUtils
import com.gembotics.ctv.ctv.VaultHelper
import com.gembotics.ctv.models.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bitcoinj.core.Address as BitcoinAddress
import org.bitcoinj.core.Coin
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.NetworkParameters
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
                
                // Parse vault JSON
                val vaultJson = Json.parseToJsonElement(request.vault).jsonObject
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
                
                // Create unvault address
                val unvaultAddress = VaultHelper.createUnvaultAddress(
                    coldCtvHash,
                    hotCtvHash,
                    blockDelay,
                    params
                )
                
                // Create vault CTV hash
                val vaultCtvHash = VaultHelper.createVaultCtv(unvaultAddress, amount, params)
                
                // Parse txid and vout
                val prevTxId = Sha256Hash.wrap(request.txid)
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
                
                val response = UnvaultingResponse(
                    vault = request.vault,
                    script = unvaultRedeemScript.toString(),
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
                
                // Parse vault JSON
                val vaultJson = Json.parseToJsonElement(request.vault).jsonObject
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
    }
}

// Extension function
private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
