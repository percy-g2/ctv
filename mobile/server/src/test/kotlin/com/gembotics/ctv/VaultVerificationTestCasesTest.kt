package com.gembotics.ctv

import com.gembotics.ctv.models.*
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Unit tests for the 7 test cases covered in VAULT_VERIFICATION_TEST_CASES.md
 */
class VaultVerificationTestCasesTest {

    @Test
    fun testCase1_VaultingEndpoint() = testApplication {
        application {
            module()
        }
        
        // Configure client with JSON serialization
        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }

        val request = VaultingRequest(
            amount = "1000000",
            coldAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
            hotAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
            blockDelay = 144,
            network = "testnet",
            taproot = false
        )

        val response = client.post("/vaults/vaulting") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        // Verify response structure
        assertTrue(json.containsKey("vault"), "Response should contain 'vault' field")
        assertTrue(json.containsKey("address"), "Response should contain 'address' field")
        
        val vault = json["vault"]?.jsonPrimitive?.content
        assertNotNull(vault, "Vault should not be null")
        
        val address = json["address"]?.jsonPrimitive?.content
        assertNotNull(address, "Address should not be null")
        assertTrue(address.isNotBlank(), "Address should not be blank")
        
        // Verify vault JSON contains expected fields
        val vaultJson = Json.parseToJsonElement(vault).jsonObject
        assertEquals("mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef", vaultJson["coldAddress"]?.jsonPrimitive?.content)
        assertEquals("mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef", vaultJson["hotAddress"]?.jsonPrimitive?.content)
        assertEquals("1000000", vaultJson["amount"]?.jsonPrimitive?.content)
        assertEquals("testnet", vaultJson["network"]?.jsonPrimitive?.content)
        assertEquals(144, vaultJson["blockDelay"]?.jsonPrimitive?.int)
        assertEquals(false, vaultJson["taproot"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun testCase2_UnvaultingEndpoint() = testApplication {
        application {
            module()
        }
        
        // Configure client with JSON serialization
        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }

        // First create a vault (Test Case 1)
        val vaultingRequest = VaultingRequest(
            amount = "1000000",
            coldAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
            hotAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
            blockDelay = 144,
            network = "testnet",
            taproot = false
        )

        val vaultingResponse = client.post("/vaults/vaulting") {
            contentType(ContentType.Application.Json)
            setBody(vaultingRequest)
        }
        val vaultJson = Json.parseToJsonElement(vaultingResponse.bodyAsText()).jsonObject["vault"]?.jsonPrimitive?.content
        assertNotNull(vaultJson)

        // Now test unvaulting (Test Case 2)
        val unvaultingRequest = UnvaultingRequest(
            vault = vaultJson,
            txid = "0000000000000000000000000000000000000000000000000000000000000000",
            vout = 0
        )

        val response = client.post("/vaults/unvaulting") {
            contentType(ContentType.Application.Json)
            setBody(unvaultingRequest)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        // Verify response structure
        assertTrue(json.containsKey("vault"), "Response should contain 'vault' field")
        assertTrue(json.containsKey("script"), "Response should contain 'script' field")
        assertTrue(json.containsKey("tx"), "Response should contain 'tx' field")
        assertTrue(json.containsKey("txid"), "Response should contain 'txid' field")
        
        val script = json["script"]?.jsonPrimitive?.content
        assertNotNull(script, "Script should not be null")
        assertTrue(script.isNotBlank(), "Script should not be blank")
        
        val tx = json["tx"]?.jsonPrimitive?.content
        assertNotNull(tx, "Transaction hex should not be null")
        assertTrue(tx.isNotBlank(), "Transaction hex should not be blank")
        
        val txid = json["txid"]?.jsonPrimitive?.content
        assertNotNull(txid, "Transaction ID should not be null")
        assertEquals(64, txid.length, "Transaction ID should be 64 hex characters")
    }

    @Test
    fun testCase3_VaultVerificationUnvaulting() = testApplication {
        application {
            module()
        }
        
        // Configure client with JSON serialization
        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }

        // First create a vault (Test Case 1)
        val vaultingRequest = VaultingRequest(
            amount = "1000000",
            coldAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
            hotAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
            blockDelay = 144,
            network = "testnet",
            taproot = false
        )

        val vaultingResponse = client.post("/vaults/vaulting") {
            contentType(ContentType.Application.Json)
            setBody(vaultingRequest)
        }
        val vaultJson = Json.parseToJsonElement(vaultingResponse.bodyAsText()).jsonObject["vault"]?.jsonPrimitive?.content
        assertNotNull(vaultJson)

        // Create unvaulting transaction (Test Case 2)
        val unvaultingRequest = UnvaultingRequest(
            vault = vaultJson,
            txid = "0000000000000000000000000000000000000000000000000000000000000000",
            vout = 0
        )

        val unvaultingResponse = client.post("/vaults/unvaulting") {
            contentType(ContentType.Application.Json)
            setBody(unvaultingRequest)
        }
        val unvaultTxHex = Json.parseToJsonElement(unvaultingResponse.bodyAsText()).jsonObject["tx"]?.jsonPrimitive?.content
        assertNotNull(unvaultTxHex)

        // Now test verification (Test Case 3)
        val verificationRequest = VaultVerificationRequest(
            vault = vaultJson,
            tx = unvaultTxHex
        )

        val response = client.post("/vaults/verification") {
            contentType(ContentType.Application.Json)
            setBody(verificationRequest)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        // Verify response structure
        assertTrue(json.containsKey("transactionType"), "Response should contain 'transactionType' field")
        assertTrue(json.containsKey("valid"), "Response should contain 'valid' field")
        assertTrue(json.containsKey("message"), "Response should contain 'message' field")
        
        val transactionType = json["transactionType"]?.jsonPrimitive?.content
        assertEquals("Unvaulting", transactionType, "Transaction type should be 'Unvaulting'")
        
        val valid = json["valid"]?.jsonPrimitive?.boolean
        assertEquals(true, valid, "Transaction should be valid")
        
        val message = json["message"]?.jsonPrimitive?.content
        assertNotNull(message, "Message should not be null")
        assertTrue(message.contains("valid unvaulting transaction", ignoreCase = true), 
            "Message should indicate valid unvaulting transaction")
    }

    @Test
    fun testCase4_SimpleCtvLocking() = testApplication {
        application {
            module()
        }
        
        // Configure client with JSON serialization
        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }

        val request = LockingRequest(
            outputs = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef:100000:",
            network = "testnet",
            congestion = false,
            taproot = false
        )

        val response = client.post("/simple/locking") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        // Verify response structure
        assertTrue(json.containsKey("ctvHash"), "Response should contain 'ctvHash' field")
        assertTrue(json.containsKey("lockingScript"), "Response should contain 'lockingScript' field")
        assertTrue(json.containsKey("lockingHex"), "Response should contain 'lockingHex' field")
        assertTrue(json.containsKey("address"), "Response should contain 'address' field")
        assertTrue(json.containsKey("ctv"), "Response should contain 'ctv' field")
        
        val ctvHash = json["ctvHash"]?.jsonPrimitive?.content
        assertNotNull(ctvHash, "CTV hash should not be null")
        assertEquals(64, ctvHash.length, "CTV hash should be 64 hex characters")
        
        val lockingScript = json["lockingScript"]?.jsonPrimitive?.content
        assertNotNull(lockingScript, "Locking script should not be null")
        assertTrue(lockingScript.isNotBlank(), "Locking script should not be blank")
        
        val lockingHex = json["lockingHex"]?.jsonPrimitive?.content
        assertNotNull(lockingHex, "Locking hex should not be null")
        assertTrue(lockingHex.isNotBlank(), "Locking hex should not be blank")
        
        val address = json["address"]?.jsonPrimitive?.content
        assertNotNull(address, "Address should not be null")
        assertTrue(address.isNotBlank(), "Address should not be blank")
        
        val ctv = json["ctv"]?.jsonPrimitive?.content
        assertNotNull(ctv, "CTV context should not be null")
        
        // Verify CTV context JSON structure
        val ctvJson = Json.parseToJsonElement(ctv).jsonObject
        assertEquals("testnet", ctvJson["network"]?.jsonPrimitive?.content)
        assertEquals(1, ctvJson["version"]?.jsonPrimitive?.int)
        assertTrue(ctvJson.containsKey("outputs"), "CTV context should contain 'outputs' field")
    }

    @Test
    fun testCase5_VaultSpendingEndpoint() = testApplication {
        application {
            module()
        }
        
        // Configure client with JSON serialization
        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }

        // First create a vault (Test Case 1)
        val vaultingRequest = VaultingRequest(
            amount = "1000000",
            coldAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
            hotAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
            blockDelay = 144,
            network = "testnet",
            taproot = false
        )

        val vaultingResponse = client.post("/vaults/vaulting") {
            contentType(ContentType.Application.Json)
            setBody(vaultingRequest)
        }
        val vaultJson = Json.parseToJsonElement(vaultingResponse.bodyAsText()).jsonObject["vault"]?.jsonPrimitive?.content
        assertNotNull(vaultJson)

        // Create unvaulting transaction (Test Case 2)
        val unvaultingRequest = UnvaultingRequest(
            vault = vaultJson,
            txid = "0000000000000000000000000000000000000000000000000000000000000000",
            vout = 0
        )

        val unvaultingResponse = client.post("/vaults/unvaulting") {
            contentType(ContentType.Application.Json)
            setBody(unvaultingRequest)
        }
        val unvaultTxid = Json.parseToJsonElement(unvaultingResponse.bodyAsText()).jsonObject["txid"]?.jsonPrimitive?.content
        assertNotNull(unvaultTxid)

        // Now test spending endpoint (Test Case 5)
        val spendingRequest = VaultSpendingRequest(
            vault = vaultJson,
            txid = unvaultTxid
        )

        val response = client.post("/vaults/spending") {
            contentType(ContentType.Application.Json)
            setBody(spendingRequest)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        // Verify response structure
        assertTrue(json.containsKey("coldTx"), "Response should contain 'coldTx' field")
        assertTrue(json.containsKey("hotTx"), "Response should contain 'hotTx' field")
        
        val coldTx = json["coldTx"]?.jsonPrimitive?.content
        assertNotNull(coldTx, "Cold transaction hex should not be null")
        assertTrue(coldTx.isNotBlank(), "Cold transaction hex should not be blank")
        
        val hotTx = json["hotTx"]?.jsonPrimitive?.content
        assertNotNull(hotTx, "Hot transaction hex should not be null")
        assertTrue(hotTx.isNotBlank(), "Hot transaction hex should not be blank")
        
        // Verify transactions are different
        assertNotEquals(coldTx, hotTx, "Cold and hot transactions should be different")
    }

    @Test
    fun testCase6_VerifyColdSpendTransaction() = testApplication {
        application {
            module()
        }
        
        // Configure client with JSON serialization
        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }

        // First create a vault (Test Case 1)
        val vaultingRequest = VaultingRequest(
            amount = "1000000",
            coldAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
            hotAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
            blockDelay = 144,
            network = "testnet",
            taproot = false
        )

        val vaultingResponse = client.post("/vaults/vaulting") {
            contentType(ContentType.Application.Json)
            setBody(vaultingRequest)
        }
        val vaultJson = Json.parseToJsonElement(vaultingResponse.bodyAsText()).jsonObject["vault"]?.jsonPrimitive?.content
        assertNotNull(vaultJson)

        // Create unvaulting transaction (Test Case 2)
        val unvaultingRequest = UnvaultingRequest(
            vault = vaultJson,
            txid = "0000000000000000000000000000000000000000000000000000000000000000",
            vout = 0
        )

        val unvaultingResponse = client.post("/vaults/unvaulting") {
            contentType(ContentType.Application.Json)
            setBody(unvaultingRequest)
        }
        val unvaultTxid = Json.parseToJsonElement(unvaultingResponse.bodyAsText()).jsonObject["txid"]?.jsonPrimitive?.content
        assertNotNull(unvaultTxid)

        // Create spending transactions (Test Case 5)
        val spendingRequest = VaultSpendingRequest(
            vault = vaultJson,
            txid = unvaultTxid
        )

        val spendingResponse = client.post("/vaults/spending") {
            contentType(ContentType.Application.Json)
            setBody(spendingRequest)
        }
        val coldTxHex = Json.parseToJsonElement(spendingResponse.bodyAsText()).jsonObject["coldTx"]?.jsonPrimitive?.content
        assertNotNull(coldTxHex)

        // Now test verification of cold spend (Test Case 6)
        val verificationRequest = VaultVerificationRequest(
            vault = vaultJson,
            tx = coldTxHex
        )

        val response = client.post("/vaults/verification") {
            contentType(ContentType.Application.Json)
            setBody(verificationRequest)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        // Verify response structure
        assertTrue(json.containsKey("transactionType"), "Response should contain 'transactionType' field")
        assertTrue(json.containsKey("valid"), "Response should contain 'valid' field")
        assertTrue(json.containsKey("message"), "Response should contain 'message' field")
        
        val transactionType = json["transactionType"]?.jsonPrimitive?.content
        assertEquals("ColdSpend", transactionType, "Transaction type should be 'ColdSpend'")
        
        val valid = json["valid"]?.jsonPrimitive?.boolean
        assertEquals(true, valid, "Transaction should be valid")
        
        val message = json["message"]?.jsonPrimitive?.content
        assertNotNull(message, "Message should not be null")
        assertTrue(message.contains("valid cold spend transaction", ignoreCase = true), 
            "Message should indicate valid cold spend transaction")
    }

    @Test
    fun testCase7_VerifyHotSpendTransaction() = testApplication {
        application {
            module()
        }
        
        // Configure client with JSON serialization
        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }

        // First create a vault (Test Case 1)
        val vaultingRequest = VaultingRequest(
            amount = "1000000",
            coldAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
            hotAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
            blockDelay = 144,
            network = "testnet",
            taproot = false
        )

        val vaultingResponse = client.post("/vaults/vaulting") {
            contentType(ContentType.Application.Json)
            setBody(vaultingRequest)
        }
        val vaultJson = Json.parseToJsonElement(vaultingResponse.bodyAsText()).jsonObject["vault"]?.jsonPrimitive?.content
        assertNotNull(vaultJson)

        // Create unvaulting transaction (Test Case 2)
        val unvaultingRequest = UnvaultingRequest(
            vault = vaultJson,
            txid = "0000000000000000000000000000000000000000000000000000000000000000",
            vout = 0
        )

        val unvaultingResponse = client.post("/vaults/unvaulting") {
            contentType(ContentType.Application.Json)
            setBody(unvaultingRequest)
        }
        val unvaultTxid = Json.parseToJsonElement(unvaultingResponse.bodyAsText()).jsonObject["txid"]?.jsonPrimitive?.content
        assertNotNull(unvaultTxid)

        // Create spending transactions (Test Case 5)
        val spendingRequest = VaultSpendingRequest(
            vault = vaultJson,
            txid = unvaultTxid
        )

        val spendingResponse = client.post("/vaults/spending") {
            contentType(ContentType.Application.Json)
            setBody(spendingRequest)
        }
        val hotTxHex = Json.parseToJsonElement(spendingResponse.bodyAsText()).jsonObject["hotTx"]?.jsonPrimitive?.content
        assertNotNull(hotTxHex)

        // Now test verification of hot spend (Test Case 7)
        val verificationRequest = VaultVerificationRequest(
            vault = vaultJson,
            tx = hotTxHex
        )

        val response = client.post("/vaults/verification") {
            contentType(ContentType.Application.Json)
            setBody(verificationRequest)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        
        // Verify response structure
        assertTrue(json.containsKey("transactionType"), "Response should contain 'transactionType' field")
        assertTrue(json.containsKey("valid"), "Response should contain 'valid' field")
        assertTrue(json.containsKey("message"), "Response should contain 'message' field")
        
        val transactionType = json["transactionType"]?.jsonPrimitive?.content
        assertEquals("HotSpend", transactionType, "Transaction type should be 'HotSpend'")
        
        val valid = json["valid"]?.jsonPrimitive?.boolean
        assertEquals(true, valid, "Transaction should be valid")
        
        val message = json["message"]?.jsonPrimitive?.content
        assertNotNull(message, "Message should not be null")
        assertTrue(message.contains("valid hot spend transaction", ignoreCase = true), 
            "Message should indicate valid hot spend transaction")
    }

    @Test
    fun testAllCasesSequentially() = testApplication {
        application {
            module()
        }
        
        // Configure client with JSON serialization
        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }

        // Test Case 1: Vaulting
        val vaultingRequest = VaultingRequest(
            amount = "1000000",
            coldAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
            hotAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
            blockDelay = 144,
            network = "testnet",
            taproot = false
        )

        val vaultingResponse = client.post("/vaults/vaulting") {
            contentType(ContentType.Application.Json)
            setBody(vaultingRequest)
        }
        assertEquals(HttpStatusCode.OK, vaultingResponse.status)
        val vaultJson = requireNotNull(Json.parseToJsonElement(vaultingResponse.bodyAsText()).jsonObject["vault"]?.jsonPrimitive?.content)

        // Test Case 2: Unvaulting
        val unvaultingRequest = UnvaultingRequest(
            vault = vaultJson,
            txid = "0000000000000000000000000000000000000000000000000000000000000000",
            vout = 0
        )

        val unvaultingResponse = client.post("/vaults/unvaulting") {
            contentType(ContentType.Application.Json)
            setBody(unvaultingRequest)
        }
        assertEquals(HttpStatusCode.OK, unvaultingResponse.status)
        val unvaultTxHex = requireNotNull(Json.parseToJsonElement(unvaultingResponse.bodyAsText()).jsonObject["tx"]?.jsonPrimitive?.content)
        val unvaultTxid = requireNotNull(Json.parseToJsonElement(unvaultingResponse.bodyAsText()).jsonObject["txid"]?.jsonPrimitive?.content)

        // Test Case 3: Vault Verification (Unvaulting)
        val verificationRequest1 = VaultVerificationRequest(
            vault = vaultJson,
            tx = unvaultTxHex
        )

        val verificationResponse1 = client.post("/vaults/verification") {
            contentType(ContentType.Application.Json)
            setBody(verificationRequest1)
        }
        assertEquals(HttpStatusCode.OK, verificationResponse1.status)
        val verificationJson1 = Json.parseToJsonElement(verificationResponse1.bodyAsText()).jsonObject
        assertEquals("Unvaulting", verificationJson1["transactionType"]?.jsonPrimitive?.content)
        assertEquals(true, verificationJson1["valid"]?.jsonPrimitive?.boolean)

        // Test Case 4: Simple CTV Locking
        val lockingRequest = LockingRequest(
            outputs = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef:100000:",
            network = "testnet",
            congestion = false,
            taproot = false
        )

        val lockingResponse = client.post("/simple/locking") {
            contentType(ContentType.Application.Json)
            setBody(lockingRequest)
        }
        assertEquals(HttpStatusCode.OK, lockingResponse.status)
        val lockingJson = Json.parseToJsonElement(lockingResponse.bodyAsText()).jsonObject
        assertTrue(lockingJson.containsKey("ctvHash"))
        assertTrue(lockingJson.containsKey("address"))

        // Test Case 5: Vault Spending
        val spendingRequest = VaultSpendingRequest(
            vault = vaultJson,
            txid = unvaultTxid
        )

        val spendingResponse = client.post("/vaults/spending") {
            contentType(ContentType.Application.Json)
            setBody(spendingRequest)
        }
        assertEquals(HttpStatusCode.OK, spendingResponse.status)
        val spendingJson = Json.parseToJsonElement(spendingResponse.bodyAsText()).jsonObject
        val coldTxHex = requireNotNull(spendingJson["coldTx"]?.jsonPrimitive?.content)
        val hotTxHex = requireNotNull(spendingJson["hotTx"]?.jsonPrimitive?.content)

        // Test Case 6: Verify Cold Spend
        val verificationRequest2 = VaultVerificationRequest(
            vault = vaultJson,
            tx = coldTxHex
        )

        val verificationResponse2 = client.post("/vaults/verification") {
            contentType(ContentType.Application.Json)
            setBody(verificationRequest2)
        }
        assertEquals(HttpStatusCode.OK, verificationResponse2.status)
        val verificationJson2 = Json.parseToJsonElement(verificationResponse2.bodyAsText()).jsonObject
        assertEquals("ColdSpend", verificationJson2["transactionType"]?.jsonPrimitive?.content)
        assertEquals(true, verificationJson2["valid"]?.jsonPrimitive?.boolean)

        // Test Case 7: Verify Hot Spend
        val verificationRequest3 = VaultVerificationRequest(
            vault = vaultJson,
            tx = hotTxHex
        )

        val verificationResponse3 = client.post("/vaults/verification") {
            contentType(ContentType.Application.Json)
            setBody(verificationRequest3)
        }
        assertEquals(HttpStatusCode.OK, verificationResponse3.status)
        val verificationJson3 = Json.parseToJsonElement(verificationResponse3.bodyAsText()).jsonObject
        assertEquals("HotSpend", verificationJson3["transactionType"]?.jsonPrimitive?.content)
        assertEquals(true, verificationJson3["valid"]?.jsonPrimitive?.boolean)
    }
}
