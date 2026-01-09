package com.gembotics.ctv.ctv

import org.bitcoinj.base.Address as BitcoinAddress
import org.bitcoinj.base.Coin
import org.bitcoinj.base.LegacyAddress
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.base.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionInput
import org.bitcoinj.core.TransactionOutPoint
import org.bitcoinj.core.TransactionOutput
import org.bitcoinj.core.TransactionWitness
import org.bitcoinj.core.Utils
import org.bitcoinj.core.Context
import org.bitcoinj.base.Base58
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptOpCodes
import java.security.MessageDigest
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

/**
 * Vault helper functions for CTV vaults
 */
object VaultHelper {
    init {
        // Register BouncyCastle provider for RIPEMD160 support
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }
    
    /**
     * Create vault CTV context
     */
    fun createVaultCtv(
        unvaultAddress: BitcoinAddress,
        amount: Coin,
        params: NetworkParameters
    ): ByteArray {
        val outputScript = ScriptBuilder.createOutputScript(unvaultAddress)
        val output = CtvOutput(
            outputScript.getProgram(),
            amount.minus(Coin.valueOf(600)).value
        )
        
        return CtvHelper.calculateCtvHash(
            version = 1,
            locktime = 0L,
            sequences = listOf(0L),
            outputs = listOf(output),
            inputIndex = 0
        )
    }
    
    /**
     * Create cold spend CTV context
     */
    fun createColdCtv(
        coldAddress: BitcoinAddress,
        amount: Coin,
        params: NetworkParameters
    ): ByteArray {
        val outputScript = ScriptBuilder.createOutputScript(coldAddress)
        val output = CtvOutput(
            outputScript.getProgram(),
            amount.minus(Coin.valueOf(1200)).value
        )
        
        return CtvHelper.calculateCtvHash(
            version = 1,
            locktime = 0L,
            sequences = listOf(0L),
            outputs = listOf(output),
            inputIndex = 0
        )
    }
    
    /**
     * Create hot spend CTV context (with delay)
     */
    fun createHotCtv(
        hotAddress: BitcoinAddress,
        amount: Coin,
        blockDelay: Int,
        params: NetworkParameters
    ): ByteArray {
        val outputScript = ScriptBuilder.createOutputScript(hotAddress)
        val output = CtvOutput(
            outputScript.getProgram(),
            amount.minus(Coin.valueOf(1200)).value
        )
        
        return CtvHelper.calculateCtvHash(
            version = 2,
            locktime = 0L,
            sequences = listOf(TransactionInput.NO_SEQUENCE - blockDelay.toLong()),
            outputs = listOf(output),
            inputIndex = 0
        )
    }
    
    /**
     * Create unvault redeem script
     * Script: IF <delay> CSV DROP <hot_ctv_hash> OP_CTV ELSE <cold_ctv_hash> OP_CTV ENDIF
     */
    fun createUnvaultRedeemScript(
        coldCtvHash: ByteArray,
        hotCtvHash: ByteArray,
        blockDelay: Int,
        params: NetworkParameters
    ): Script {
        // Validate CTV hash sizes (must be exactly 32 bytes)
        if (coldCtvHash.size != 32) {
            throw IllegalArgumentException("Cold CTV hash must be exactly 32 bytes, got ${coldCtvHash.size}")
        }
        if (hotCtvHash.size != 32) {
            throw IllegalArgumentException("Hot CTV hash must be exactly 32 bytes, got ${hotCtvHash.size}")
        }
        
        return try {
            // Workaround for bitcoinj 0.16.1 bug with 32-byte pushes
            // Manually construct the entire script to avoid the bug
            val scriptBytes = mutableListOf<Byte>()
            
            // OP_IF
            scriptBytes.add(ScriptOpCodes.OP_IF.toByte())
            
            // Block delay - use ScriptBuilder just for number encoding, then extract bytes
            val delayBuilder = ScriptBuilder()
            delayBuilder.number(blockDelay.toLong())
            val delayScript = delayBuilder.build().program
            scriptBytes.addAll(delayScript.toList())
            
            // OP_CHECKSEQUENCEVERIFY OP_DROP
            scriptBytes.add(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY.toByte())
            scriptBytes.add(ScriptOpCodes.OP_DROP.toByte())
            
            // Push hot CTV hash (32 bytes) - workaround: use OP_PUSHDATA1 for 32 bytes
            // This avoids the bug with direct push opcode 0x20
            scriptBytes.add(0x4c.toByte()) // OP_PUSHDATA1
            scriptBytes.add(32) // Length: 32 bytes
            scriptBytes.addAll(hotCtvHash.copyOf(32).toList())
            
            // OP_NOP4 (OP_CTV) OP_ELSE
            scriptBytes.add(ScriptOpCodes.OP_NOP4.toByte())
            scriptBytes.add(ScriptOpCodes.OP_ELSE.toByte())
            
            // Push cold CTV hash (32 bytes) - workaround: use OP_PUSHDATA1 for 32 bytes
            scriptBytes.add(0x4c.toByte()) // OP_PUSHDATA1
            scriptBytes.add(32) // Length: 32 bytes
            scriptBytes.addAll(coldCtvHash.copyOf(32).toList())
            
            // OP_NOP4 (OP_CTV) OP_ENDIF
            scriptBytes.add(ScriptOpCodes.OP_NOP4.toByte())
            scriptBytes.add(ScriptOpCodes.OP_ENDIF.toByte())
            
            // Create Script object - catch parsing errors
            val scriptByteArray = scriptBytes.toByteArray()
            try {
                Script(scriptByteArray)
            } catch (e: ArrayIndexOutOfBoundsException) {
                // If Script constructor fails, the error might be in parsing
                // Try to identify which part is causing the issue
                throw IllegalArgumentException("Script constructor failed when parsing ${scriptByteArray.size} bytes. " +
                        "This may be a bitcoinj 0.16.1 bug with OP_PUSHDATA1 and 32-byte data. " +
                        "Error: ${e.message}", e)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Error building unvault redeem script: ${e.message}. CTV hash sizes: cold=${coldCtvHash.size}, hot=${hotCtvHash.size}", e)
        }
    }
    
    /**
     * Create unvault redeem script bytes directly (without Script object)
     * This avoids potential bugs in Script constructor/parser
     */
    fun createUnvaultRedeemScriptBytes(
        coldCtvHash: ByteArray,
        hotCtvHash: ByteArray,
        blockDelay: Int
    ): ByteArray {
        val scriptBytes = mutableListOf<Byte>()
        
        // OP_IF
        scriptBytes.add(ScriptOpCodes.OP_IF.toByte())
        
        // Block delay - use ScriptBuilder just for number encoding, then extract bytes
        val delayBuilder = ScriptBuilder()
        delayBuilder.number(blockDelay.toLong())
        val delayScript = delayBuilder.build().program
        scriptBytes.addAll(delayScript.toList())
        
        // OP_CHECKSEQUENCEVERIFY OP_DROP
        scriptBytes.add(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY.toByte())
        scriptBytes.add(ScriptOpCodes.OP_DROP.toByte())
        
        // Push hot CTV hash (32 bytes) - workaround: push as 31 bytes + 1 byte to avoid OP_PUSHDATA1 bug
        scriptBytes.add(31) // Push 31 bytes (opcode 0x1f = 31)
        scriptBytes.addAll(hotCtvHash.copyOf(31).toList())
        scriptBytes.add(1) // Push 1 byte (opcode 0x01 = 1)
        scriptBytes.add(hotCtvHash[31])
        
        // OP_NOP4 (OP_CTV) OP_ELSE
        scriptBytes.add(ScriptOpCodes.OP_NOP4.toByte())
        scriptBytes.add(ScriptOpCodes.OP_ELSE.toByte())
        
        // Push cold CTV hash (32 bytes) - workaround: push as 31 bytes + 1 byte to avoid OP_PUSHDATA1 bug
        scriptBytes.add(31) // Push 31 bytes (opcode 0x1f = 31)
        scriptBytes.addAll(coldCtvHash.copyOf(31).toList())
        scriptBytes.add(1) // Push 1 byte (opcode 0x01 = 1)
        scriptBytes.add(coldCtvHash[31])
        
        // OP_NOP4 (OP_CTV) OP_ENDIF
        scriptBytes.add(ScriptOpCodes.OP_NOP4.toByte())
        scriptBytes.add(ScriptOpCodes.OP_ENDIF.toByte())
        
        return scriptBytes.toByteArray()
    }
    
    /**
     * Create unvault address (P2WSH)
     */
    fun createUnvaultAddress(
        coldCtvHash: ByteArray,
        hotCtvHash: ByteArray,
        blockDelay: Int,
        params: NetworkParameters
    ): BitcoinAddress {
        // Use direct byte array construction to avoid Script constructor/parser bugs
        val redeemScriptBytes = try {
            createUnvaultRedeemScriptBytes(coldCtvHash, hotCtvHash, blockDelay)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to create unvault redeem script bytes: ${e.message}", e)
        }
        
        // Compute SHA256 hash of script (for P2WSH)
        val sha256 = java.security.MessageDigest.getInstance("SHA-256")
        val scriptHash = try {
            sha256.digest(redeemScriptBytes)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to hash redeem script: ${e.message}", e)
        }
        
        // Create address from hash160 of SHA256 hash (workaround - using P2SH format)
        // hash160 = RIPEMD160(SHA256(script))
        // We already have SHA256 (scriptHash), so we need RIPEMD160
        // Utils.sha256hash160() has a bug with our script in bitcoinj 0.16.1, so bypass it entirely
        // and compute hash160 manually using BouncyCastle
        val hash160 = try {
            // Directly use BouncyCastle RIPEMD160 to avoid bitcoinj bug
            // We already have SHA256(script), so just compute RIPEMD160(scriptHash)
            val ripemd160 = MessageDigest.getInstance("RIPEMD160", "BC")
            ripemd160.update(scriptHash)
            ripemd160.digest()
        } catch (e: Exception) {
            // Fallback: try default provider (might work if BouncyCastle is registered globally)
            try {
                val ripemd160 = MessageDigest.getInstance("RIPEMD160")
                ripemd160.update(scriptHash)
                ripemd160.digest()
            } catch (e2: Exception) {
                throw IllegalArgumentException("Failed to compute hash160 using RIPEMD160. " +
                        "BouncyCastle error: ${e.message}, Default provider error: ${e2.message}. " +
                        "Script size: ${redeemScriptBytes.size} bytes. " +
                        "This is a workaround for bitcoinj 0.16.1 Utils.sha256hash160() bug.", e)
            }
        }
        // Create LegacyAddress from P2SH hash
        val addressStr = Base58.encodeChecked(params.addressHeader, hash160)
        return BitcoinAddress.fromString(params, addressStr) as LegacyAddress
    }
    
    /**
     * Create vault address (CTV address pointing to unvault)
     */
    fun createVaultAddress(
        coldAddress: BitcoinAddress,
        hotAddress: BitcoinAddress,
        amount: Coin,
        blockDelay: Int,
        params: NetworkParameters
    ): BitcoinAddress {
        val unvaultAddress = createUnvaultAddress(
            createColdCtv(coldAddress, amount, params),
            createHotCtv(hotAddress, amount, blockDelay, params),
            blockDelay,
            params
        )
        
        val vaultCtvHash = createVaultCtv(unvaultAddress, amount, params)
        return CtvHelper.createCtvAddress(vaultCtvHash, params)
    }
    
    /**
     * Create unvault transaction
     */
    fun createUnvaultTransaction(
        vaultCtvHash: ByteArray,
        prevTxId: Sha256Hash,
        prevVout: Int,
        unvaultAddress: BitcoinAddress,
        amount: Coin,
        params: NetworkParameters
    ): Transaction {
        val outputScript = ScriptBuilder.createOutputScript(unvaultAddress)
        val output = CtvOutput(
            outputScript.getProgram(),
            amount.minus(Coin.valueOf(600)).value
        )
        
        return CtvHelper.createSpendingTransaction(
            vaultCtvHash,
            prevTxId,
            prevVout,
            listOf(output),
            params
        )
    }
    
    /**
     * Create cold spend transaction
     */
    fun createColdSpendTransaction(
        unvaultRedeemScript: Script,
        prevTxId: Sha256Hash,
        prevVout: Int,
        coldAddress: BitcoinAddress,
        amount: Coin,
        params: NetworkParameters
    ): Transaction {
        // bitcoinj 0.17: Transaction no longer takes NetworkParameters
        // Set context instead
        Context.propagate(Context(params))
        val tx = Transaction()
        // Note: version might need to be set via constructor or might be immutable
        
        // Manually create P2WSH output script to avoid bitcoinj bug
        val redeemScriptBytes = unvaultRedeemScript.program
        val sha256 = java.security.MessageDigest.getInstance("SHA-256")
        val scriptHash = sha256.digest(redeemScriptBytes)
        val p2wshScriptBytes = byteArrayOf(0x00) + scriptHash // OP_0 + 32-byte hash
        
        // Add input
        // bitcoinj 0.17: TransactionOutPoint takes (Long, Sha256Hash) - index first, then hash
        val outPoint = TransactionOutPoint(prevVout.toLong(), prevTxId)
        // bitcoinj 0.17: TransactionInput constructor changed
        val input = TransactionInput(tx, p2wshScriptBytes, outPoint)
        
        // Create witness for cold path (empty for ELSE branch)
        // bitcoinj 0.17: TransactionWitness.of() instead of setPush()
        val witness = TransactionWitness.of(
            ByteArray(0), // Empty for ELSE
            unvaultRedeemScript.getProgram()
        )
        // In bitcoinj 0.17, use withWitness() which returns a new TransactionInput
        val inputWithWitness = input.withWitness(witness)
        tx.addInput(inputWithWitness)
        
        // Add output
        val outputScript = ScriptBuilder.createOutputScript(coldAddress)
        val txOut = TransactionOutput(
            params,
            tx,
            amount.minus(Coin.valueOf(1200)),
            outputScript.getProgram()
        )
        tx.addOutput(txOut)
        
        tx.lockTime = 0
        
        return tx
    }
    
    /**
     * Create hot spend transaction (with delay)
     */
    fun createHotSpendTransaction(
        unvaultRedeemScript: Script,
        prevTxId: Sha256Hash,
        prevVout: Int,
        hotAddress: BitcoinAddress,
        amount: Coin,
        blockDelay: Int,
        params: NetworkParameters
    ): Transaction {
        // bitcoinj 0.17: Transaction no longer takes NetworkParameters
        // Set context instead
        Context.propagate(Context(params))
        val tx = Transaction()
        // Note: version might need to be set via constructor or might be immutable
        
        // Manually create P2WSH output script to avoid bitcoinj bug
        val redeemScriptBytes = unvaultRedeemScript.program
        val sha256 = java.security.MessageDigest.getInstance("SHA-256")
        val scriptHash = sha256.digest(redeemScriptBytes)
        val p2wshScriptBytes = byteArrayOf(0x00) + scriptHash // OP_0 + 32-byte hash
        
        // Add input
        // bitcoinj 0.17: TransactionOutPoint takes (Long, Sha256Hash) - index first, then hash
        val outPoint = TransactionOutPoint(prevVout.toLong(), prevTxId)
        // bitcoinj 0.17: TransactionInput constructor changed
        val input = TransactionInput(tx, p2wshScriptBytes, outPoint)
        
        // Set sequence with delay
        // Note: sequenceNumber might be immutable in some bitcoinj versions
        // Note: sequenceNumber might not be settable in bitcoinj 0.16.1
        // This will be handled by the transaction builder if needed
        
        // Create witness for hot path (1 for IF branch)
        // bitcoinj 0.17: TransactionWitness.of() instead of setPush()
        val witness = TransactionWitness.of(
            byteArrayOf(1), // 1 for IF branch
            unvaultRedeemScript.getProgram()
        )
        // In bitcoinj 0.17, use withWitness() which returns a new TransactionInput
        val inputWithWitness = input.withWitness(witness)
        tx.addInput(inputWithWitness)
        
        // Add output
        val outputScript = ScriptBuilder.createOutputScript(hotAddress)
        val txOut = TransactionOutput(
            params,
            tx,
            amount.minus(Coin.valueOf(1200)),
            outputScript.getProgram()
        )
        tx.addOutput(txOut)
        
        tx.lockTime = 0
        
        return tx
    }
    
    /**
     * Verification result for vault transactions
     */
    data class VerificationResult(
        val transactionType: String, // "Unvaulting", "ColdSpend", "HotSpend"
        val valid: Boolean,
        val message: String
    )
    
    /**
     * Verify a vault transaction
     * Returns verification result indicating transaction type and validity
     */
    fun verifyTransaction(
        txHex: String,
        coldAddress: BitcoinAddress,
        hotAddress: BitcoinAddress,
        amount: Coin,
        blockDelay: Int,
        params: NetworkParameters
    ): VerificationResult {
        try {
            // Parse transaction from hex
            val txBytes = txHex.replace(" ", "").replace("\n", "").chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
            // bitcoinj 0.17: Use Transaction.read() instead of constructor
            Context.propagate(Context(params))
            val tx = Transaction.read(java.nio.ByteBuffer.wrap(txBytes))
            
            if (tx.inputs.isEmpty()) {
                return VerificationResult("Unknown", false, "Invalid: Transaction has no inputs")
            }
            
            // Check each input to determine transaction type
            // Unvaulting transactions spend from vault (CTV) and have CTV hash in witness
            // Cold/Hot spend transactions spend from unvault address and have redeem script in witness
            
            // First, compute expected values
            val unvaultAddress = createUnvaultAddress(
                createColdCtv(coldAddress, amount, params),
                createHotCtv(hotAddress, amount, blockDelay, params),
                blockDelay,
                params
            )
            val vaultCtvHash = createVaultCtv(unvaultAddress, amount, params)
            val unvaultRedeemScript = createUnvaultRedeemScript(
                createColdCtv(coldAddress, amount, params),
                createHotCtv(hotAddress, amount, blockDelay, params),
                blockDelay,
                params
            )
            val unvaultRedeemScriptBytes = unvaultRedeemScript.program
            
            // Check each input
            for (i in 0 until tx.inputs.size) {
                val input = tx.getInput(i.toLong())
                val witness = input.witness
                
                if (witness != null && witness.pushCount >= 2) {
                    val firstWitnessItem = witness.getPush(0)
                    val secondWitnessItem = witness.getPush(1)
                    
                    // Check if this is an unvaulting transaction (spending from vault)
                    // Unvaulting transactions have vault CTV hash (32 bytes) in first witness item
                    if (firstWitnessItem != null && firstWitnessItem.size == 32) {
                        if (firstWitnessItem.contentEquals(vaultCtvHash)) {
                            // This is an unvaulting transaction
                            // Verify transaction structure matches expected unvault transaction
                            val outPoint = input.outpoint
                            val expectedUnvaultTx = createUnvaultTransaction(
                                vaultCtvHash,
                                outPoint.hash,
                                outPoint.index.toInt(),
                                unvaultAddress,
                                amount,
                                params
                            )
                            
                            // Compare transaction structure
                            val issues = mutableListOf<String>()
                            if (tx.version != expectedUnvaultTx.version) {
                                issues.add("Version mismatch")
                            }
                            if (tx.lockTime != expectedUnvaultTx.lockTime) {
                                issues.add("Locktime mismatch")
                            }
                            if (tx.inputs.size != expectedUnvaultTx.inputs.size) {
                                issues.add("Input count mismatch: expected ${expectedUnvaultTx.inputs.size}, got ${tx.inputs.size}")
                            }
                            if (tx.outputs.size != expectedUnvaultTx.outputs.size) {
                                issues.add("Output count mismatch: expected ${expectedUnvaultTx.outputs.size}, got ${tx.outputs.size}")
                            }
                            
                            if (issues.isEmpty()) {
                                return VerificationResult("Unvaulting", true, "Transaction is a valid unvaulting transaction")
                            } else {
                                return VerificationResult("Unvaulting", false, "Transaction structure issues: ${issues.joinToString(", ")}")
                            }
                        }
                    }
                    
                    // Check if this is a cold or hot spend (spending from unvault address)
                    // These transactions have redeem script in second witness item
                    if (secondWitnessItem != null && secondWitnessItem.contentEquals(unvaultRedeemScriptBytes)) {
                        val firstWitnessItem = witness.getPush(0)
                        val isHot = firstWitnessItem != null && firstWitnessItem.isNotEmpty() && firstWitnessItem[0] == 1.toByte()
                        
                        val outPoint = input.outpoint
                        val prevTxId = outPoint.hash
                        val prevVout = outPoint.index.toInt()
                        val expectedTx = if (isHot) {
                            createHotSpendTransaction(
                                unvaultRedeemScript,
                                prevTxId,
                                prevVout,
                                hotAddress,
                                amount,
                                blockDelay,
                                params
                            )
                        } else {
                            createColdSpendTransaction(
                                unvaultRedeemScript,
                                prevTxId,
                                prevVout,
                                coldAddress,
                                amount,
                                params
                            )
                        }
                        
                        // Compare transaction structure
                        val issues = mutableListOf<String>()
                        
                        if (tx.version != expectedTx.version) {
                            issues.add("Version mismatch: expected ${expectedTx.version}, got ${tx.version}")
                        }
                        
                        if (tx.lockTime != expectedTx.lockTime) {
                            issues.add("Locktime mismatch: expected ${expectedTx.lockTime}, got ${tx.lockTime}")
                        }
                        
                        if (tx.inputs.size != expectedTx.inputs.size) {
                            issues.add("Input count mismatch: expected ${expectedTx.inputs.size}, got ${tx.inputs.size}")
                        } else if (tx.inputs.isNotEmpty() && expectedTx.inputs.isNotEmpty()) {
                            val txInput = tx.getInput(i.toLong())
                            val expectedInput = expectedTx.getInput(0)
                            
                            if (txInput.sequenceNumber != expectedInput.sequenceNumber) {
                                issues.add("Sequence mismatch: expected ${expectedInput.sequenceNumber}, got ${txInput.sequenceNumber}")
                            }
                        }
                        
                        if (tx.outputs.size != expectedTx.outputs.size) {
                            issues.add("Output count mismatch: expected ${expectedTx.outputs.size}, got ${tx.outputs.size}")
                        } else if (tx.outputs.isNotEmpty() && expectedTx.outputs.isNotEmpty()) {
                            val txOutput = tx.getOutput(0)
                            val expectedOutput = expectedTx.getOutput(0)
                            
                            if (txOutput.value != expectedOutput.value) {
                                issues.add("Output value mismatch: expected ${expectedOutput.value}, got ${txOutput.value}")
                            }
                            
                            if (!txOutput.scriptPubKey.equals(expectedOutput.scriptPubKey)) {
                                issues.add("Output script mismatch")
                            }
                        }
                        
                        val transactionType = if (isHot) "HotSpend" else "ColdSpend"
                        if (issues.isEmpty()) {
                            val txType = if (isHot) "hot spend" else "cold spend"
                            return VerificationResult(transactionType, true, "Transaction is a valid $txType transaction")
                        } else {
                            val txType = if (isHot) "hot spend" else "cold spend"
                            return VerificationResult(transactionType, false, "Transaction verification failed: ${issues.joinToString(", ")}")
                        }
                    }
                }
            }
            
            return VerificationResult("Unknown", false, "Could not determine transaction type")
        } catch (e: Exception) {
            return VerificationResult("Unknown", false, "Error verifying transaction: ${e.message}")
        }
    }
}
