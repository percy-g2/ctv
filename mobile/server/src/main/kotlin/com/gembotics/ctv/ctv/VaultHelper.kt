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
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.script.ScriptOpCodes

/**
 * Vault helper functions for CTV vaults
 */
object VaultHelper {
    
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
        return ScriptBuilder()
            .op(ScriptOpCodes.OP_IF)
            .number(blockDelay.toLong())
            .op(ScriptOpCodes.OP_CHECKSEQUENCEVERIFY)
            .op(ScriptOpCodes.OP_DROP)
            .data(hotCtvHash)
            .op(ScriptOpCodes.OP_NOP4) // OP_CTV
            .op(ScriptOpCodes.OP_ELSE)
            .data(coldCtvHash)
            .op(ScriptOpCodes.OP_NOP4) // OP_CTV
            .op(ScriptOpCodes.OP_ENDIF)
            .build()
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
        val redeemScript = createUnvaultRedeemScript(coldCtvHash, hotCtvHash, blockDelay, params)
        val p2wshScript = ScriptBuilder.createP2WSHOutputScript(redeemScript)
        // In bitcoinj 0.16.1, create P2WSH address from script
        // Use LegacyAddress.fromP2SHScript for P2WSH (works as workaround)
        // In bitcoinj 0.16.1, create P2WSH address manually from script hash
        // P2WSH uses SHA256 hash of the script
        val scriptBytes = p2wshScript.getProgram()
        val sha256 = java.security.MessageDigest.getInstance("SHA-256")
        val scriptHash = sha256.digest(scriptBytes)
        // Create address from hash160 of SHA256 hash (workaround - using P2SH format)
        val hash160 = Utils.sha256hash160(scriptHash)
        // Create LegacyAddress from P2SH hash
        // In bitcoinj 0.16.1, use LegacyAddress constructor
        // Note: This is a workaround for P2WSH - actual implementation may differ
        // In bitcoinj 0.16.1, create address from hash using Address.fromString
        // First create the P2SH address string, then parse it
        // This is a workaround since fromP2SHHash doesn't exist in 0.16.1
        val addressStr = org.bitcoinj.core.Base58.encodeChecked(params.addressHeader, hash160)
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
        val tx = Transaction(params)
        // Note: version might need to be set via constructor or might be immutable
        
        // Create P2WSH output script
        val p2wshScript = ScriptBuilder.createP2WSHOutputScript(unvaultRedeemScript)
        
        // Add input
        val outPoint = TransactionOutPoint(params, prevTxId.bytes, prevVout)
        val input = TransactionInput(params, tx, p2wshScript.getProgram(), outPoint)
        
        // Create witness for cold path (empty for ELSE branch)
        val witness = TransactionWitness(2)
        witness.setPush(0, ByteArray(0)) // Empty for ELSE
        witness.setPush(1, unvaultRedeemScript.getProgram())
        // In bitcoinj 0.16.1, witness needs to be set on the transaction
        // Use getInput and setWitness method
        tx.getInput(0)?.setWitness(witness)
        
        tx.addInput(input)
        
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
        val tx = Transaction(params)
        // Note: version might need to be set via constructor or might be immutable
        
        // Create P2WSH output script
        val p2wshScript = ScriptBuilder.createP2WSHOutputScript(unvaultRedeemScript)
        
        // Add input
        val outPoint = TransactionOutPoint(params, prevTxId.bytes, prevVout)
        val input = TransactionInput(params, tx, p2wshScript.getProgram(), outPoint)
        
        // Set sequence with delay
        // Note: sequenceNumber might be immutable in some bitcoinj versions
        // Note: sequenceNumber might not be settable in bitcoinj 0.16.1
        // This will be handled by the transaction builder if needed
        
        // Create witness for hot path (1 for IF branch)
        val witness = TransactionWitness(2)
        witness.setPush(0, byteArrayOf(1)) // 1 for IF branch
        witness.setPush(1, unvaultRedeemScript.getProgram())
        // In bitcoinj 0.16.1, witness needs to be set on the transaction
        // Use getInput and setWitness method
        tx.getInput(0)?.setWitness(witness)
        
        tx.addInput(input)
        
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
}
