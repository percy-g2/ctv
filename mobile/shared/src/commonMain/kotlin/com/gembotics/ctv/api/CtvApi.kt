package com.gembotics.ctv.api

import com.gembotics.ctv.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * API client for CTV Playground
 * 
 * This is a common interface that can be implemented for different platforms
 */
interface CtvApi {
    suspend fun simpleLocking(request: LockingRequest): LockingResponse
    suspend fun simpleSpending(request: SpendingRequest): SpendingResponse
    suspend fun vaultVaulting(request: VaultingRequest): VaultingResponse
    suspend fun vaultUnvaulting(request: UnvaultingRequest): UnvaultingResponse
    suspend fun vaultSpending(request: VaultSpendingRequest): VaultSpendingResponse
    suspend fun vaultVerification(request: VaultVerificationRequest): VaultVerificationResponse
}

/**
 * Default implementation using Ktor HttpClient
 */
expect fun createHttpClient(): HttpClient

fun createCtvApi(baseUrl: String): CtvApi = CtvApiImpl(baseUrl)

class CtvApiImpl(private val baseUrl: String) : CtvApi {
    private val client = createHttpClient()
    
    override suspend fun simpleLocking(request: LockingRequest): LockingResponse {
        return client.post("$baseUrl/simple/locking") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
    
    override suspend fun simpleSpending(request: SpendingRequest): SpendingResponse {
        return client.post("$baseUrl/simple/spending") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
    
    override suspend fun vaultVaulting(request: VaultingRequest): VaultingResponse {
        return client.post("$baseUrl/vaults/vaulting") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
    
    override suspend fun vaultUnvaulting(request: UnvaultingRequest): UnvaultingResponse {
        return client.post("$baseUrl/vaults/unvaulting") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
    
    override suspend fun vaultSpending(request: VaultSpendingRequest): VaultSpendingResponse {
        return client.post("$baseUrl/vaults/spending") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
    
    override suspend fun vaultVerification(request: VaultVerificationRequest): VaultVerificationResponse {
        return client.post("$baseUrl/vaults/verification") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
