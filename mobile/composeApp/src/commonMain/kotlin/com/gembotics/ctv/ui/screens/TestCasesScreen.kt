package com.gembotics.ctv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gembotics.ctv.api.createCtvApi
import com.gembotics.ctv.models.*
import kotlinx.coroutines.launch

data class TestResult(
    val testNumber: Int,
    val testName: String,
    val status: TestStatus,
    val message: String,
    val details: String? = null
)

enum class TestStatus {
    PENDING,
    RUNNING,
    PASSED,
    FAILED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestCasesScreen(
    apiBaseUrl: String,
    onBack: () -> Unit
) {
    val api = remember { createCtvApi(apiBaseUrl) }
    val coroutineScope = rememberCoroutineScope()
    
    var testResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }
    var isRunning by remember { mutableStateOf(false) }
    var overallStatus by remember { mutableStateOf<TestStatus?>(null) }
    
    // Initialize test results
    LaunchedEffect(Unit) {
        testResults = listOf(
            TestResult(1, "Vaulting Endpoint", TestStatus.PENDING, "Creates a new vault and returns the vault address"),
            TestResult(2, "Unvaulting Endpoint", TestStatus.PENDING, "Creates an unvaulting transaction"),
            TestResult(3, "Vault Verification (Unvaulting)", TestStatus.PENDING, "Verifies unvaulting transaction"),
            TestResult(4, "Simple CTV Locking", TestStatus.PENDING, "Creates a simple CTV locking script"),
            TestResult(5, "Vault Spending Endpoint", TestStatus.PENDING, "Creates spending transactions"),
            TestResult(6, "Verify Cold Spend", TestStatus.PENDING, "Verifies cold spend transaction"),
            TestResult(7, "Verify Hot Spend", TestStatus.PENDING, "Verifies hot spend transaction")
        )
    }
    
    fun updateTestResult(index: Int, status: TestStatus, message: String, details: String? = null) {
        testResults = testResults.toMutableList().apply {
            this[index] = this[index].copy(status = status, message = message, details = details)
        }
    }
    
    fun runAllTests() {
        if (isRunning) return
        
        isRunning = true
        overallStatus = null
        
        // Reset all tests to pending
        testResults = testResults.map { it.copy(status = TestStatus.PENDING, message = it.message, details = null) }
        
        coroutineScope.launch {
            try {
                // Test 1: Vaulting
                updateTestResult(0, TestStatus.RUNNING, "Running...")
                val vaultingRequest = VaultingRequest(
                    amount = "1000000",
                    coldAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
                    hotAddress = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
                    blockDelay = 144,
                    network = "testnet",
                    taproot = false
                )
                val vaultingResponse = api.vaultVaulting(vaultingRequest)
                val vaultJson = vaultingResponse.vault
                updateTestResult(0, TestStatus.PASSED, "Vault created successfully", 
                    "Address: ${vaultingResponse.address}\nVault: $vaultJson")
                
                // Test 2: Unvaulting
                updateTestResult(1, TestStatus.RUNNING, "Running...")
                val unvaultingRequest = UnvaultingRequest(
                    vault = vaultJson,
                    txid = "0000000000000000000000000000000000000000000000000000000000000000",
                    vout = 0
                )
                val unvaultingResponse = api.vaultUnvaulting(unvaultingRequest)
                val unvaultTxHex = unvaultingResponse.tx
                val unvaultTxid = unvaultingResponse.txid
                updateTestResult(1, TestStatus.PASSED, "Unvaulting transaction created", 
                    "TXID: $unvaultTxid")
                
                // Test 3: Vault Verification (Unvaulting)
                updateTestResult(2, TestStatus.RUNNING, "Running...")
                val verificationRequest1 = VaultVerificationRequest(
                    vault = vaultJson,
                    tx = unvaultTxHex
                )
                val verificationResponse1 = api.vaultVerification(verificationRequest1)
                val passed1 = verificationResponse1.valid && verificationResponse1.transactionType == "Unvaulting"
                updateTestResult(2, if (passed1) TestStatus.PASSED else TestStatus.FAILED,
                    verificationResponse1.message,
                    "Type: ${verificationResponse1.transactionType}, Valid: ${verificationResponse1.valid}")
                
                // Test 4: Simple CTV Locking
                updateTestResult(3, TestStatus.RUNNING, "Running...")
                val lockingRequest = LockingRequest(
                    outputs = "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef:100000:",
                    network = "testnet",
                    congestion = false,
                    taproot = false
                )
                val lockingResponse = api.simpleLocking(lockingRequest)
                updateTestResult(3, TestStatus.PASSED, "CTV lock created successfully",
                    "Address: ${lockingResponse.address}\nCTvHash: ${lockingResponse.ctvHash}")
                
                // Test 5: Vault Spending
                updateTestResult(4, TestStatus.RUNNING, "Running...")
                val spendingRequest = VaultSpendingRequest(
                    vault = vaultJson,
                    txid = unvaultTxid
                )
                val spendingResponse = api.vaultSpending(spendingRequest)
                updateTestResult(4, TestStatus.PASSED, "Spending transactions created",
                    "Cold and Hot transactions generated")
                
                // Test 6: Verify Cold Spend
                updateTestResult(5, TestStatus.RUNNING, "Running...")
                val verificationRequest2 = VaultVerificationRequest(
                    vault = vaultJson,
                    tx = spendingResponse.coldTx
                )
                val verificationResponse2 = api.vaultVerification(verificationRequest2)
                val passed2 = verificationResponse2.valid && verificationResponse2.transactionType == "ColdSpend"
                updateTestResult(5, if (passed2) TestStatus.PASSED else TestStatus.FAILED,
                    verificationResponse2.message,
                    "Type: ${verificationResponse2.transactionType}, Valid: ${verificationResponse2.valid}")
                
                // Test 7: Verify Hot Spend
                updateTestResult(6, TestStatus.RUNNING, "Running...")
                val verificationRequest3 = VaultVerificationRequest(
                    vault = vaultJson,
                    tx = spendingResponse.hotTx
                )
                val verificationResponse3 = api.vaultVerification(verificationRequest3)
                val passed3 = verificationResponse3.valid && verificationResponse3.transactionType == "HotSpend"
                updateTestResult(6, if (passed3) TestStatus.PASSED else TestStatus.FAILED,
                    verificationResponse3.message,
                    "Type: ${verificationResponse3.transactionType}, Valid: ${verificationResponse3.valid}")
                
                // Determine overall status
                val allPassed = testResults.all { it.status == TestStatus.PASSED }
                overallStatus = if (allPassed) TestStatus.PASSED else TestStatus.FAILED
                
            } catch (e: Exception) {
                // Find the first running test and mark it as failed
                val runningIndex = testResults.indexOfFirst { it.status == TestStatus.RUNNING }
                if (runningIndex >= 0) {
                    updateTestResult(runningIndex, TestStatus.FAILED, "Error: ${e.message}")
                }
                overallStatus = TestStatus.FAILED
            } finally {
                isRunning = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault Verification Test Cases") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Overall status card
            overallStatus?.let { status ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (status) {
                            TestStatus.PASSED -> MaterialTheme.colorScheme.primaryContainer
                            TestStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Overall Status",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            when (status) {
                                TestStatus.PASSED -> "✓ All Tests Passed"
                                TestStatus.FAILED -> "✗ Some Tests Failed"
                                else -> "Running..."
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = when (status) {
                                TestStatus.PASSED -> MaterialTheme.colorScheme.primary
                                TestStatus.FAILED -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Run all tests button
            Button(
                onClick = { runAllTests() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRunning
            ) {
                if (isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Running Tests...")
                } else {
                    Text("Run All 7 Test Cases")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Test results
            testResults.forEachIndexed { index, result ->
                TestResultCard(result)
                if (index < testResults.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun TestResultCard(result: TestResult) {
    val cardColor = when (result.status) {
        TestStatus.PASSED -> MaterialTheme.colorScheme.primaryContainer
        TestStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        TestStatus.RUNNING -> MaterialTheme.colorScheme.secondaryContainer
        TestStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = when (result.status) {
        TestStatus.PASSED -> MaterialTheme.colorScheme.onPrimaryContainer
        TestStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
        TestStatus.RUNNING -> MaterialTheme.colorScheme.onSecondaryContainer
        TestStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Test ${result.testNumber}: ${result.testName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    when (result.status) {
                        TestStatus.PASSED -> "✓"
                        TestStatus.FAILED -> "✗"
                        TestStatus.RUNNING -> "⟳"
                        TestStatus.PENDING -> "○"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                result.message,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            
            result.details?.let { details ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    details,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
