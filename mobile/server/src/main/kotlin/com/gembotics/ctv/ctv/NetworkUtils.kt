package com.gembotics.ctv.ctv

import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.params.RegTestParams

/**
 * Network parameter utilities
 */
object NetworkUtils {
    fun getNetworkParams(network: String): NetworkParameters {
        return when (network.lowercase()) {
            "bitcoin", "mainnet" -> MainNetParams.get()
            "testnet" -> TestNet3Params.get()
            "regtest" -> RegTestParams.get()
            "signet" -> TestNet3Params.get() // SigNet not available in 0.16.1, use testnet
            else -> TestNet3Params.get() // Default to testnet
        }
    }
}
