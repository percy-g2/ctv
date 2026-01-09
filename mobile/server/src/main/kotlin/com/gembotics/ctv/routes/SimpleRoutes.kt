package com.gembotics.ctv.routes

import com.gembotics.ctv.ctv.CtvHelper
import com.gembotics.ctv.ctv.CtvOutput
import com.gembotics.ctv.ctv.NetworkUtils
import com.gembotics.ctv.models.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bitcoinj.core.Address as BitcoinAddress
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.script.ScriptBuilder
import kotlinx.serialization.json.*
import java.math.BigInteger

/**
 * Simple CTV routes with bitcoinj implementation
 */
fun Route.simpleRoutes() {
    route("/simple") {
        get {
            call.respond(mapOf("message" to "Simple CTV Playground"))
        }
        
        post("/locking") {
            try {
                val request = call.receive<LockingRequest>()
                val params = NetworkUtils.getNetworkParams(request.network)
                
                // Parse outputs (format: address:amount:data)
                val outputs = mutableListOf<CtvOutput>()
                val addresses = mutableListOf<BitcoinAddress>()
                val amounts = mutableListOf<Coin>()
                
                if (request.outputs.isBlank()) {
                    throw IllegalArgumentException("Outputs cannot be blank")
                }
                
                for (line in request.outputs.lines()) {
                    if (line.isBlank()) continue
                    
                    val parts = line.split(":")
                    if (parts.size < 2) {
                        throw IllegalArgumentException("Invalid output format: '$line'. Expected: address:amount:data")
                    }
                    
                    val addressStr = parts[0].trim()
                    val amountStr = parts[1].trim()
                    val dataStr = if (parts.size > 2) parts[2].trim() else ""
                    
                    if (addressStr.isBlank()) {
                        throw IllegalArgumentException("Address cannot be blank in line: '$line'")
                    }
                    if (amountStr.isBlank()) {
                        throw IllegalArgumentException("Amount cannot be blank in line: '$line'")
                    }
                    
                    val address = try {
                        BitcoinAddress.fromString(params, addressStr)
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Invalid address '$addressStr' for network '${request.network}'. Note: bc1 addresses are for mainnet, tb1 for testnet. Error: ${e.message}", e)
                    }
                    
                    val amount = try {
                        Coin.valueOf(amountStr.toLong())
                    } catch (e: NumberFormatException) {
                        throw IllegalArgumentException("Invalid amount '$amountStr': not a valid number", e)
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Invalid amount '$amountStr': ${e.message}", e)
                    }
                    
                    // Subtract fee (600 satoshis)
                    val finalAmount = amount.minus(Coin.valueOf(600))
                    
                    addresses.add(address)
                    amounts.add(finalAmount)
                    
                    // Create script pubkey for output
                    val outputScript = ScriptBuilder.createOutputScript(address)
                    outputs.add(CtvOutput(outputScript.getProgram(), finalAmount.value))
                    
                    // Add data output if present (only if dataStr is not empty)
                    if (dataStr.isNotBlank()) {
                        try {
                            val dataBytes = dataStr.hexToByteArray()
                            if (dataBytes.isNotEmpty()) {
                                val dataScript = ScriptBuilder.createOpReturnScript(dataBytes)
                                outputs.add(CtvOutput(dataScript.getProgram(), 0))
                            }
                        } catch (e: Exception) {
                            throw IllegalArgumentException("Invalid hex data: '$dataStr'. Error: ${e.message}", e)
                        }
                    }
                }
                
                // Calculate CTV hash
                val version = 1
                val locktime = 0L
                val sequences = listOf(0L)
                val inputIndex = 0
                
                val ctvHash = CtvHelper.calculateCtvHash(
                    version, locktime, sequences, outputs, inputIndex
                )
                
                // Create locking script and address
                val lockingScript = CtvHelper.createCtvLockingScript(ctvHash, params)
                val ctvAddress = CtvHelper.createCtvAddress(ctvHash, params)
                
                // Create CTV context JSON
                val ctvContext = buildJsonObject {
                    put("network", request.network)
                    put("version", version)
                    put("locktime", locktime)
                    put("sequences", buildJsonArray { sequences.forEach { add(it) } })
                    put("inputIndex", inputIndex)
                    put("ctvHash", ctvHash.toHex())
                    put("outputs", buildJsonArray {
                        outputs.forEach { output ->
                            add(buildJsonObject {
                                put("amount", output.amount)
                                put("scriptPubKey", output.scriptPubKey.toHex())
                            })
                        }
                    })
                }
                
                val response = LockingResponse(
                    ctvHash = ctvHash.toHex(),
                    lockingScript = lockingScript.toString(),
                    lockingHex = lockingScript.program.toHex(),
                    address = ctvAddress.toString(),
                    ctv = ctvContext.toString()
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
                val request = call.receive<SpendingRequest>()
                
                // Parse CTV context from JSON
                val ctvJson = Json.parseToJsonElement(request.ctv).jsonObject
                val network = ctvJson["network"]?.jsonPrimitive?.content ?: "testnet"
                val params = NetworkUtils.getNetworkParams(network)
                
                val ctvHash = ctvJson["ctvHash"]?.jsonPrimitive?.content?.hexToByteArray()
                    ?: throw IllegalArgumentException("Missing ctvHash in CTV context")
                
                val outputsJson = ctvJson["outputs"]?.jsonArray
                    ?: throw IllegalArgumentException("Missing outputs in CTV context")
                
                val outputs = outputsJson.map { outputObj ->
                    val obj = outputObj.jsonObject
                    val amount = obj["amount"]?.jsonPrimitive?.long ?: 0L
                    val scriptHex = obj["scriptPubKey"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("Missing scriptPubKey in output")
                    val scriptPubKey = scriptHex.hexToByteArray()
                    CtvOutput(scriptPubKey, amount)
                }
                
                // Parse txid and vout
                val prevTxId = Sha256Hash.wrap(request.txid)
                val prevVout = request.vout
                
                // Create spending transaction
                val spendingTx = CtvHelper.createSpendingTransaction(
                    ctvHash,
                    prevTxId,
                    prevVout,
                    outputs,
                    params
                )
                
                // Serialize transaction
                val txHex = spendingTx.bitcoinSerialize().toHex()
                
                val response = SpendingResponse(
                    txs = listOf(txHex)
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

// Extension functions
private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
private fun String.hexToByteArray(): ByteArray {
    if (isEmpty()) return ByteArray(0)
    if (length % 2 != 0) {
        throw IllegalArgumentException("Invalid hex string: length must be even, got $length")
    }
    return try {
        chunked(2).map { 
            if (it.length != 2) throw IllegalArgumentException("Invalid hex pair: '$it'")
            it.toInt(16).toByte() 
        }.toByteArray()
    } catch (e: NumberFormatException) {
        throw IllegalArgumentException("Invalid hex character in string: $this", e)
    }
}
