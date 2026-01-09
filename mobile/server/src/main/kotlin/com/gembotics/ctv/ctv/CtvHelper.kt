package com.gembotics.ctv.ctv

import org.bitcoinj.base.Address as BitcoinAddress
import org.bitcoinj.base.Coin
import org.bitcoinj.base.LegacyAddress
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.base.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.Context
import org.bitcoinj.core.TransactionInput
import org.bitcoinj.core.TransactionOutPoint
import org.bitcoinj.core.TransactionOutput
import org.bitcoinj.core.TransactionWitness
import org.bitcoinj.core.Utils
import org.bitcoinj.base.Base58
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptOpCodes
import java.security.MessageDigest

/**
 * CTV (CheckTemplateVerify) helper functions
 * CTV uses OP_NOP4 which would become OP_CTV if activated
 */
object CtvHelper {
    
    /**
     * Calculate CTV hash from transaction template
     */
    fun calculateCtvHash(
        version: Int,
        locktime: Long,
        sequences: List<Long>,
        outputs: List<CtvOutput>,
        inputIndex: Int
    ): ByteArray {
        // CTV hash calculation: SHA256(SHA256(tx_template))
        val template = buildTxTemplate(version, locktime, sequences, outputs, inputIndex)
        val sha256 = MessageDigest.getInstance("SHA-256")
        val firstHash = sha256.digest(template)
        return sha256.digest(firstHash)
    }
    
    /**
     * Build transaction template for CTV hash calculation
     */
    private fun buildTxTemplate(
        version: Int,
        locktime: Long,
        sequences: List<Long>,
        outputs: List<CtvOutput>,
        inputIndex: Int
    ): ByteArray {
        val stream = UnsafeByteArrayOutputStream()
        
        // Version (4 bytes, little-endian)
        writeUint32LE(version.toLong(), stream)
        
        // Input count (varint, but CTV uses 1)
        stream.write(1)
        
        // Previous output hash (32 bytes, all zeros for template)
        stream.writeBytes(ByteArray(32))
        
        // Previous output index (4 bytes, all zeros)
        writeUint32LE(0, stream)
        
        // Sequence for this input
        val sequence = sequences.getOrElse(inputIndex) { 0 }
        writeUint32LE(sequence, stream)
        
        // Output count (varint)
        writeVarInt(outputs.size.toLong(), stream)
        
        // Outputs
        for (output in outputs) {
            // Value (8 bytes, little-endian)
            writeInt64LE(output.amount, stream)
            
            // Script length (varint)
            writeVarInt(output.scriptPubKey.size.toLong(), stream)
            
            // Script
            stream.writeBytes(output.scriptPubKey)
        }
        
        // Locktime (4 bytes, little-endian)
        writeUint32LE(locktime, stream)
        
        return stream.toByteArray()
    }
    
    private fun writeUint32LE(value: Long, stream: UnsafeByteArrayOutputStream) {
        val bytes = ByteArray(4)
        bytes[0] = (value and 0xFF).toByte()
        bytes[1] = ((value shr 8) and 0xFF).toByte()
        bytes[2] = ((value shr 16) and 0xFF).toByte()
        bytes[3] = ((value shr 24) and 0xFF).toByte()
        stream.writeBytes(bytes)
    }
    
    private fun writeInt64LE(value: Long, stream: UnsafeByteArrayOutputStream) {
        val bytes = ByteArray(8)
        for (i in 0 until 8) {
            bytes[i] = ((value shr (i * 8)) and 0xFF).toByte()
        }
        stream.writeBytes(bytes)
    }
    
    /**
     * Write varint to stream
     */
    private fun writeVarInt(value: Long, stream: UnsafeByteArrayOutputStream) {
        when {
            value < 0xFD -> stream.write(value.toInt())
            value <= 0xFFFF -> {
                stream.write(0xFD)
                val bytes = ByteArray(2)
                bytes[0] = (value and 0xFF).toByte()
                bytes[1] = ((value shr 8) and 0xFF).toByte()
                stream.writeBytes(bytes)
            }
            value <= 0xFFFFFFFFL -> {
                stream.write(0xFE)
                writeUint32LE(value, stream)
            }
            else -> {
                stream.write(0xFF)
                writeInt64LE(value, stream)
            }
        }
    }
    
    /**
     * Create CTV locking script (P2WSH)
     */
    fun createCtvLockingScript(ctvHash: ByteArray, params: NetworkParameters): Script {
        // Validate CTV hash size (must be exactly 32 bytes)
        if (ctvHash.size != 32) {
            throw IllegalArgumentException("CTV hash must be exactly 32 bytes, got ${ctvHash.size}")
        }
        
        return try {
            // Workaround for bitcoinj 0.16.1 bug with 32-byte pushes
            // Manually construct script bytes to avoid the bug
            val scriptBytes = mutableListOf<Byte>()
            
            // Push CTV hash (32 bytes) - workaround: use OP_PUSHDATA1 for 32 bytes
            // This avoids the bug with direct push opcode 0x20
            scriptBytes.add(0x4c.toByte()) // OP_PUSHDATA1
            scriptBytes.add(32) // Length: 32 bytes
            scriptBytes.addAll(ctvHash.copyOf(32).toList())
            
            // OP_NOP4 (OP_CTV)
            scriptBytes.add(ScriptOpCodes.OP_NOP4.toByte())
            
            Script(scriptBytes.toByteArray())
        } catch (e: Exception) {
            throw IllegalArgumentException("Error building CTV locking script: ${e.message}. CTV hash size: ${ctvHash.size}", e)
        }
    }
    
    /**
     * Create P2WSH address from CTV script
     */
    fun createCtvAddress(ctvHash: ByteArray, params: NetworkParameters): BitcoinAddress {
        val ctvScript = createCtvLockingScript(ctvHash, params)
        
        // Manually create P2WSH output script to avoid bitcoinj bug
        // P2WSH: OP_0 <32-byte-script-hash>
        val ctvScriptBytes = ctvScript.program
        val sha256 = MessageDigest.getInstance("SHA-256")
        val scriptHash = sha256.digest(ctvScriptBytes)
        
        // Create address from hash160 of SHA256 hash (workaround - using P2SH format)
        // hash160 = RIPEMD160(SHA256(script))
        // We already have SHA256 (scriptHash), so compute RIPEMD160 manually using BouncyCastle
        val hash160 = try {
            val ripemd160 = MessageDigest.getInstance("RIPEMD160", "BC")
            ripemd160.update(scriptHash)
            ripemd160.digest()
        } catch (e: Exception) {
            try {
                val ripemd160 = MessageDigest.getInstance("RIPEMD160")
                ripemd160.update(scriptHash)
                ripemd160.digest()
            } catch (e2: Exception) {
                throw IllegalArgumentException("Failed to compute hash160 using RIPEMD160. " +
                        "BouncyCastle error: ${e.message}, Default provider error: ${e2.message}. " +
                        "Script size: ${ctvScriptBytes.size} bytes.", e)
            }
        }
        // Create LegacyAddress from P2SH hash  
        val addressStr = Base58.encodeChecked(params.addressHeader, hash160)
        return BitcoinAddress.fromString(params, addressStr) as LegacyAddress
    }
    
    /**
     * Create spending transaction from CTV template
     */
    fun createSpendingTransaction(
        ctvHash: ByteArray,
        prevTxId: Sha256Hash,
        prevVout: Int,
        outputs: List<CtvOutput>,
        params: NetworkParameters
    ): Transaction {
        // bitcoinj 0.17: Transaction no longer takes NetworkParameters
        // Set context instead
        Context.propagate(Context(params))
        val tx = Transaction()
        // Note: version might need to be set via constructor or might be immutable
        
        // Create CTV script and manually create P2WSH output script to avoid bitcoinj bug
        val ctvScript = createCtvLockingScript(ctvHash, params)
        val ctvScriptBytes = ctvScript.program
        val sha256 = MessageDigest.getInstance("SHA-256")
        val scriptHash = sha256.digest(ctvScriptBytes)
        val p2wshScriptBytes = byteArrayOf(0x00) + scriptHash // OP_0 + 32-byte hash
        
        // Add input with witness
        // bitcoinj 0.17: TransactionOutPoint takes (Long, Sha256Hash) - index first, then hash
        val outPoint = TransactionOutPoint(prevVout.toLong(), prevTxId)
        // bitcoinj 0.17: TransactionInput constructor changed
        val input = TransactionInput(tx, p2wshScriptBytes, outPoint)
        
        // Create witness for CTV spend (CTV hash + OP_CTV script)
        // bitcoinj 0.17: TransactionWitness.of() instead of setPush()
        val witness = TransactionWitness.of(
            ctvHash,
            ctvScript.getProgram()
        )
        // In bitcoinj 0.17, use withWitness() which returns a new TransactionInput
        // Only add the input once, with witness attached
        val inputWithWitness = input.withWitness(witness)
        tx.addInput(inputWithWitness)
        
        // Add outputs
        for (output in outputs) {
            val script = Script(output.scriptPubKey)
            val txOut = TransactionOutput(params, tx, Coin.valueOf(output.amount), script.getProgram())
            tx.addOutput(txOut)
        }
        
        tx.lockTime = 0
        
        return tx
    }
}

/**
 * CTV output representation
 */
data class CtvOutput(
    val scriptPubKey: ByteArray,
    val amount: Long // in satoshis
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as CtvOutput
        
        if (!scriptPubKey.contentEquals(other.scriptPubKey)) return false
        if (amount != other.amount) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = scriptPubKey.contentHashCode()
        result = 31 * result + amount.hashCode()
        return result
    }
}

/**
 * Helper class for byte array output stream
 */
private class UnsafeByteArrayOutputStream : java.io.ByteArrayOutputStream() {
    override fun write(value: Int) {
        super.write(value)
    }
    
    override fun writeBytes(bytes: ByteArray) {
        super.write(bytes, 0, bytes.size)
    }
}
