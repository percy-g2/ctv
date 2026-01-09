package com.gembotics.ctv.api

import com.gembotics.ctv.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }
    
    private suspend inline fun <reified T> HttpResponse.handleResponse(): T {
        val status = status
        
        if (!status.isSuccess()) {
            // Try to parse error message from response
            val bodyText = try {
                bodyAsText()
            } catch (e: Exception) {
                "Unable to read response body"
            }
            val errorMessage = try {
                val jsonObj = json.parseToJsonElement(bodyText).jsonObject
                jsonObj["error"]?.jsonPrimitive?.content ?: bodyText
            } catch (e: Exception) {
                bodyText.ifEmpty { "Server returned ${status.value} ${status.description}" }
            }
            throw ApiException(status.value, errorMessage)
        }
        
        // Use ContentNegotiation plugin for deserialization
        // Note: If this fails, we can't read bodyAsText() again since body() consumes the stream
        return try {
            body()
        } catch (e: Exception) {
            throw ApiException(
                status.value,
                "Failed to parse response: ${e.message}"
            )
        }
    }
    
    override suspend fun simpleLocking(request: LockingRequest): LockingResponse {
        return client.post("$baseUrl/simple/locking") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.handleResponse()
    }
    
    override suspend fun simpleSpending(request: SpendingRequest): SpendingResponse {
        return client.post("$baseUrl/simple/spending") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.handleResponse()
    }
    
    override suspend fun vaultVaulting(request: VaultingRequest): VaultingResponse {
        return client.post("$baseUrl/vaults/vaulting") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.handleResponse()
    }
    
    override suspend fun vaultUnvaulting(request: UnvaultingRequest): UnvaultingResponse {
        return client.post("$baseUrl/vaults/unvaulting") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.handleResponse()
    }
    
    override suspend fun vaultSpending(request: VaultSpendingRequest): VaultSpendingResponse {
        return client.post("$baseUrl/vaults/spending") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.handleResponse()
    }
    
    override suspend fun vaultVerification(request: VaultVerificationRequest): VaultVerificationResponse {
        return client.post("$baseUrl/vaults/verification") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.handleResponse()
    }
}

/**
 * Exception thrown when API requests fail
 */
class ApiException(val statusCode: Int, message: String) : Exception("API Error ($statusCode): $message")
