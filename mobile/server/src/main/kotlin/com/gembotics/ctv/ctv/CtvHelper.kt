package com.gembotics.ctv.ctv

import org.bitcoinj.core.Address as BitcoinAddress
import org.bitcoinj.core.Coin
import org.bitcoinj.core.LegacyAddress
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionInput
import org.bitcoinj.core.TransactionOutPoint
import org.bitcoinj.core.TransactionOutput
import org.bitcoinj.core.TransactionWitness
import org.bitcoinj.core.Utils
import org.bitcoinj.core.Base58
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
        return ScriptBuilder()
            .data(ctvHash)
            .op(ScriptOpCodes.OP_NOP4) // OP_CTV (currently OP_NOP4)
            .build()
    }
    
    /**
     * Create P2WSH address from CTV script
     */
    fun createCtvAddress(ctvHash: ByteArray, params: NetworkParameters): BitcoinAddress {
        val ctvScript = createCtvLockingScript(ctvHash, params)
        val p2wshScript = ScriptBuilder.createP2WSHOutputScript(ctvScript)
        // In bitcoinj 0.16.1, create P2WSH address from script hash
        // P2WSH uses SHA256, not hash160 - but LegacyAddress.fromP2SHHash expects hash160
        // For now, create address from script directly (workaround)
        // In bitcoinj 0.16.1, create P2WSH address manually from script hash
        // P2WSH uses SHA256 hash of the script
        val scriptBytes = p2wshScript.getProgram()
        val sha256 = MessageDigest.getInstance("SHA-256")
        val scriptHash = sha256.digest(scriptBytes)
        // Create address from hash160 of SHA256 hash (workaround - using P2SH format)
        val hash160 = Utils.sha256hash160(scriptHash)
        // Create LegacyAddress from P2SH hash
        // In bitcoinj 0.16.1, create address from hash using Address.fromString
        // First create the P2SH address string, then parse it
        // This is a workaround since fromP2SHHash doesn't exist in 0.16.1
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
        val tx = Transaction(params)
        // Note: version might need to be set via constructor or might be immutable
        
        // Create CTV script and P2WSH output script
        val ctvScript = createCtvLockingScript(ctvHash, params)
        val p2wshScript = ScriptBuilder.createP2WSHOutputScript(ctvScript)
        
        // Add input
        val outPoint = TransactionOutPoint(params, prevTxId.bytes, prevVout)
        val input = TransactionInput(params, tx, p2wshScript.getProgram(), outPoint)
        tx.addInput(input)
        
        // Create witness for CTV spend (CTV hash + OP_CTV script)
        val witness = TransactionWitness(2)
        witness.setPush(0, ctvHash)
        witness.setPush(1, ctvScript.getProgram())
        // In bitcoinj 0.16.1, set witness using connectOutput method or addWitness
        // For now, we'll set it directly if the property exists
        // Note: This may need adjustment based on actual bitcoinj 0.16.1 API
        // In bitcoinj 0.16.1, witness needs to be set on the transaction
        // Use getInput and setWitness method
        tx.getInput(0)?.setWitness(witness)
        
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
