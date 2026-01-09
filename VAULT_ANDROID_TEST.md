# Android Vault Testing Guide

This document provides comprehensive test cases and step-by-step instructions for testing vault functionality in the Android CTV Playground application.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Setup Instructions](#setup-instructions)
3. [Test Cases](#test-cases)
4. [End-to-End Test Flow](#end-to-end-test-flow)
5. [Edge Cases and Error Scenarios](#edge-cases-and-error-scenarios)
6. [Test Data](#test-data)

---

## Prerequisites

### Required Software

- **Android Studio** (latest version recommended)
- **JDK 17+** (Java Development Kit)
- **Android SDK** (installed via Android Studio)
- **Android Emulator** or physical Android device for testing
- **Gradle** (included with Android Studio)

### Server Setup

- The Ktor server must be running before testing the Android app
- Server runs on port `8080` by default
- Android emulator connects to `http://10.0.2.2:8080` (maps to `localhost` on host machine)
- Physical devices need the host machine's local IP address (e.g., `http://192.168.1.x:8080`)

### Test Data Preparation

- **Testnet addresses** for safe testing (addresses starting with `tb1...`, `m...`, `n...`, or `2...`)
- Sample testnet addresses are provided in the [Test Data](#test-data) section
- Ensure you have a way to copy/paste JSON data between screens

---

## Setup Instructions

### Step 1: Start the Ktor Server

1. Open a terminal and navigate to the mobile directory:
   ```bash
   cd mobile
   ```

2. Start the Ktor server:
   ```bash
   ./gradlew :server:run
   ```

3. Verify the server is running:
   - You should see server startup logs
   - Server should be accessible at `http://localhost:8080`
   - Keep this terminal window open while testing

### Step 2: Configure Android App (if needed)

1. Open the project in Android Studio
2. Navigate to `mobile/composeApp/src/commonMain/kotlin/com/gembotics/ctv/Constants.kt`
3. Verify the API base URL:
   - For emulator: `http://10.0.2.2:8080` (default)
   - For physical device: Update to your computer's local IP (e.g., `http://192.168.1.100:8080`)

### Step 3: Run the Android App

1. **Open Android Studio** and select the `mobile` directory as the project
2. **Set up Android Emulator** (if not already set up):
   - Go to Tools → Device Manager
   - Create a new virtual device (recommended: Pixel 5 or newer with API 33+)
   - Start the emulator
3. **Run the app**:
   - Select the `composeApp` configuration from the run configurations dropdown
   - Click the Run button (or press `Shift+F10`)
   - The app will build and install on your emulator/device

### Step 4: Verify Connection

1. Open the app on your device/emulator
2. Navigate to any screen that makes an API call
3. If you see an error about connection, verify:
   - Server is running on port 8080
   - You're using an emulator (for `10.0.2.2`) or correct IP for physical device
   - No firewall is blocking the connection

---

## Test Cases

### Test Case 1: Create Vault

**Objective**: Test the vault creation functionality with various configurations.

#### Test 1.1: Create Segwit Vault (Default)

**Steps**:
1. Open the app and tap **"CTV Vaults"** from the home screen
2. Tap **"Create Vault"**
3. Fill in the following fields:
   - **Amount**: `1000000` (1,000,000 satoshis = 0.01 BTC)
   - **Cold Address**: `mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef`
   - **Hot Address**: `tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx`
   - **Block Delay**: `144` (approximately 1 day)
   - **Network**: Select `testnet` from dropdown
   - **Taproot**: Leave unchecked (false)
4. Tap **"Create Vault"** button

**Expected Results**:
- Loading indicator appears briefly
- Result card displays:
  - **Vault Address**: A valid testnet address (e.g., `mgDrrGRPouPXEWhgzuwDXQZRovnNv1GF5x`)
  - **Vault Context**: A JSON string containing vault configuration
- No error messages displayed
- Vault Context JSON should contain:
  ```json
  {
    "hot": "tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx",
    "cold": "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
    "amount": "1000000",
    "network": "testnet",
    "delay": 144,
    "taproot": false
  }
  ```

**Verification**:
- Copy the Vault Context JSON for use in subsequent tests
- Verify the vault address is a valid testnet address format
- Verify all input values are correctly reflected in the JSON

#### Test 1.2: Create Taproot Vault

**Steps**:
1. Navigate to **"Create Vault"** screen
2. Fill in the same fields as Test 1.1
3. **Enable Taproot**: Check the Taproot checkbox
4. Tap **"Create Vault"**

**Expected Results**:
- Vault address is generated successfully
- Vault Context JSON shows `"taproot": true`
- Address format may differ (Taproot addresses start with `tb1p...`)

**Verification**:
- Verify `taproot: true` in the JSON response
- Compare address format with Segwit vault address

#### Test 1.3: Create Vault with Different Block Delays

**Steps**:
1. Create vaults with different block delay values:
   - `6` (1 hour)
   - `144` (1 day)
   - `1008` (1 week)
   - `4320` (1 month)

**Expected Results**:
- All vaults create successfully
- Block delay value is correctly reflected in the Vault Context JSON
- Vault addresses are generated for each configuration

**Verification**:
- Verify delay values match input values in JSON
- Note that different delays don't affect the vault address generation

#### Test 1.4: Create Vault with Different Networks

**Steps**:
1. Test creating vaults with different network options:
   - `testnet`
   - `signet`
   - `regtest`
   - `bitcoin` (mainnet - use with caution)

**Expected Results**:
- Each network generates appropriate address formats
- Network value is correctly stored in Vault Context JSON
- Address formats match network selection

**Verification**:
- Testnet: Addresses start with `m`, `n`, `2`, or `tb1`
- Signet: Similar to testnet
- Regtest: Similar to testnet
- Mainnet: Addresses start with `1`, `3`, or `bc1`

#### Test 1.5: Validation Tests

**Steps**:
1. Try to create vault with empty fields:
   - Empty amount field
   - Empty cold address
   - Empty hot address
2. Try invalid addresses:
   - Invalid Bitcoin address format
   - Mainnet address with testnet network selected
3. Try invalid amount:
   - Negative numbers
   - Non-numeric values
   - Zero amount

**Expected Results**:
- **Create Vault** button should be disabled when required fields are empty
- Error messages displayed for invalid inputs
- No vault created with invalid data

**Verification**:
- UI prevents submission with invalid data
- Error messages are clear and helpful

---

### Test Case 2: Unvault Funds

**Objective**: Test creating an unvaulting transaction that moves funds from the vault to an unvault address.

#### Test 2.1: Unvault with Valid Vault Context

**Prerequisites**: Complete Test Case 1.1 and save the Vault Context JSON.

**Steps**:
1. Navigate to **"CTV Vaults"** → **"Unvault Funds"**
2. Paste the **Vault Context JSON** from Test Case 1.1 into the "Vault Context" field
3. Enter the following:
   - **Transaction ID (TXID)**: `0000000000000000000000000000000000000000000000000000000000000000` (placeholder for testing)
   - **Output Index (Vout)**: `0`
4. Tap **"Create Unvaulting Transaction"**

**Expected Results**:
- Loading indicator appears
- Result card displays:
  - **Unvault Redeem Script**: A hex-encoded script (e.g., `63029000b2751f01f427d7b0a6334728a5dc3b1b3d92f1e024ea7d9a430c41f29b1e33a4e59101dcb3671f43d591e156a317ac5010a620ef3c7001c53b459e60ba885fdbfbbe1bec34d201b7b368`)
  - **Unvaulting Transaction**: Hex-encoded transaction (starts with `01000000000102...`)
  - **Unvaulting TXID**: Transaction ID hash
  - **Updated Vault Context**: JSON string (same as input, used for next step)

**Verification**:
- Copy the Updated Vault Context JSON for Test Case 3
- Copy the Unvaulting TXID for Test Case 3
- Verify transaction hex is valid (64 characters per line, hex characters only)
- Verify TXID is 64-character hex string

#### Test 2.2: Unvault with Different Vout Values

**Steps**:
1. Use the same vault context from Test 2.1
2. Test with different vout values: `0`, `1`, `2`

**Expected Results**:
- All vout values generate valid unvaulting transactions
- Transaction structure changes based on vout (different input index)

**Verification**:
- Verify transactions are valid for each vout value
- Note that vout must match the actual output index of the funding transaction

#### Test 2.3: Unvault Validation Tests

**Steps**:
1. Try unvaulting with:
   - Empty vault context field
   - Invalid JSON format
   - Empty TXID
   - Invalid TXID format (not 64 hex characters)
   - Empty vout
   - Negative vout value

**Expected Results**:
- **Create Unvaulting Transaction** button disabled for empty required fields
- Error messages for invalid inputs
- No transaction created with invalid data

**Verification**:
- UI validation prevents invalid submissions
- Error messages are descriptive

---

### Test Case 3: Spend from Vault

**Objective**: Test creating spending transactions (cold and hot) from an unvault transaction.

#### Test 3.1: Generate Cold and Hot Spend Transactions

**Prerequisites**: Complete Test Cases 1.1 and 2.1. Save the Updated Vault Context JSON and Unvaulting TXID.

**Steps**:
1. Navigate to **"CTV Vaults"** → **"Spend from Vault"**
2. Paste the **Updated Vault Context JSON** from Test Case 2.1 into the "Vault Context" field
3. Enter the **Unvaulting Transaction ID** from Test Case 2.1
4. Tap **"Create Spending Transactions"**

**Expected Results**:
- Loading indicator appears
- Result card displays:
  - **Cold Spend Transaction**: Hex-encoded transaction (can be spent immediately, no delay)
  - **Hot Spend Transaction**: Hex-encoded transaction (requires block delay before spending)

**Verification**:
- Both transactions are valid hex-encoded Bitcoin transactions
- Cold transaction has version `0x01` and sequence `0x00000000`
- Hot transaction has version `0x02` and sequence matching the block delay
- Copy both transaction hex values for verification testing

#### Test 3.2: Verify Transaction Differences

**Steps**:
1. Compare the cold and hot spend transactions from Test 3.1
2. Note the differences in transaction structure

**Expected Results**:
- **Cold Spend**: 
  - Transaction version: `0x01`
  - Sequence: `0x00000000` (no delay)
  - Can be broadcast immediately
- **Hot Spend**:
  - Transaction version: `0x02`
  - Sequence: Contains block delay (e.g., `0x00000090` for 144 blocks)
  - Requires waiting for block delay before broadcasting

**Verification**:
- Transaction versions differ as expected
- Sequence values reflect the delay configuration
- Both transactions spend from the same unvaulting transaction output

#### Test 3.3: Spend Validation Tests

**Steps**:
1. Try spending with:
   - Empty vault context
   - Invalid JSON format
   - Empty unvaulting TXID
   - Invalid TXID format
   - Mismatched vault context (from different vault)

**Expected Results**:
- Button disabled for empty required fields
- Error messages for invalid inputs
- No transactions generated with invalid data

**Verification**:
- Proper validation prevents errors
- Error messages guide user to fix issues

---

### Test Case 4: Verify Vault Transaction

**Objective**: Test the transaction verification functionality for unvaulting, cold spend, and hot spend transactions.

#### Test 4.1: Verify Unvaulting Transaction

**Prerequisites**: Complete Test Cases 1.1 and 2.1. Save the Vault Context JSON and Unvaulting Transaction hex.

**Steps**:
1. Navigate to **"CTV Vaults"** → **"Verify Vault Transaction"**
2. Paste the **Vault Context JSON** from Test Case 1.1
3. Paste the **Unvaulting Transaction hex** from Test Case 2.1
4. Tap **"Verify Transaction"**

**Expected Results**:
- Loading indicator appears
- Result card displays:
  - **Verification Result**: Green/valid indicator
  - **Transaction Type**: `"Unvaulting"`
  - **Valid**: `true`
  - **Message**: "Transaction is a valid unvaulting transaction"

**Verification**:
- Verification succeeds for valid unvaulting transaction
- Transaction type is correctly identified
- UI shows success state (green/valid indicator)

#### Test 4.2: Verify Cold Spend Transaction

**Prerequisites**: Complete Test Case 3.1. Save the Cold Spend Transaction hex and Vault Context JSON.

**Steps**:
1. Navigate to **"Verify Vault Transaction"**
2. Paste the **Vault Context JSON** from Test Case 1.1
3. Paste the **Cold Spend Transaction hex** from Test Case 3.1
4. Tap **"Verify Transaction"**

**Expected Results**:
- Result card displays:
  - **Transaction Type**: `"ColdSpend"`
  - **Valid**: `true`
  - **Message**: "Transaction is a valid cold spend transaction"

**Verification**:
- Cold spend transaction verified successfully
- Transaction type correctly identified as ColdSpend
- Verification message confirms immediate spend capability

#### Test 4.3: Verify Hot Spend Transaction

**Prerequisites**: Complete Test Case 3.1. Save the Hot Spend Transaction hex.

**Steps**:
1. Navigate to **"Verify Vault Transaction"**
2. Paste the **Vault Context JSON** from Test Case 1.1
3. Paste the **Hot Spend Transaction hex** from Test Case 3.1
4. Tap **"Verify Transaction"**

**Expected Results**:
- Result card displays:
  - **Transaction Type**: `"HotSpend"`
  - **Valid**: `true`
  - **Message**: "Transaction is a valid hot spend transaction"

**Verification**:
- Hot spend transaction verified successfully
- Transaction type correctly identified as HotSpend
- Verification confirms block delay requirement

#### Test 4.4: Verify Invalid Transaction

**Steps**:
1. Use valid vault context
2. Paste an invalid transaction hex:
   - Random hex string
   - Transaction from different vault
   - Malformed transaction hex
3. Tap **"Verify Transaction"**

**Expected Results**:
- Error message displayed
- Result shows:
  - **Valid**: `false`
  - **Message**: Error description explaining why verification failed

**Verification**:
- Invalid transactions are correctly rejected
- Error messages are informative
- UI shows error state (red/invalid indicator)

#### Test 4.5: Verify with Mismatched Vault Context

**Steps**:
1. Create two different vaults (Vault A and Vault B)
2. Create an unvaulting transaction for Vault A
3. Try to verify Vault A's transaction with Vault B's context

**Expected Results**:
- Verification fails
- Error message indicates mismatch
- **Valid**: `false`

**Verification**:
- System correctly identifies vault context mismatch
- Prevents verification of transactions with wrong context

---

## End-to-End Test Flow

This section provides a complete workflow test that exercises all vault functionality in sequence.

### Complete Vault Workflow Test

**Objective**: Test the complete vault lifecycle from creation to spending.

#### Step 1: Create a Vault

1. Open app → Tap **"CTV Vaults"** → Tap **"Create Vault"**
2. Enter vault parameters:
   ```
   Amount: 1000000
   Cold Address: mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef
   Hot Address: tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx
   Block Delay: 144
   Network: testnet
   Taproot: false
   ```
3. Tap **"Create Vault"**
4. **Copy the Vault Context JSON** from the result
5. **Copy the Vault Address** (you would send funds to this address in a real scenario)

**Checkpoint**: Verify vault address is generated and JSON contains all input values.

#### Step 2: Unvault Funds

1. Navigate back → Tap **"Unvault Funds"**
2. Paste the **Vault Context JSON** from Step 1
3. Enter transaction details:
   ```
   TXID: 0000000000000000000000000000000000000000000000000000000000000000
   Vout: 0
   ```
   > **Note**: In production, use the actual TXID of the transaction that funded your vault address.
4. Tap **"Create Unvaulting Transaction"**
5. **Copy the Updated Vault Context JSON**
6. **Copy the Unvaulting TXID**
7. **Copy the Unvaulting Transaction hex** (for verification)

**Checkpoint**: Verify unvaulting transaction is generated with valid hex format.

#### Step 3: Verify Unvaulting Transaction

1. Navigate back → Tap **"Verify Vault Transaction"**
2. Paste the **Vault Context JSON** from Step 1
3. Paste the **Unvaulting Transaction hex** from Step 2
4. Tap **"Verify Transaction"**

**Checkpoint**: Verification should show:
- Transaction Type: `"Unvaulting"`
- Valid: `true`
- Success message displayed

#### Step 4: Generate Spending Transactions

1. Navigate back → Tap **"Spend from Vault"**
2. Paste the **Updated Vault Context JSON** from Step 2
3. Paste the **Unvaulting TXID** from Step 2
4. Tap **"Create Spending Transactions"**
5. **Copy the Cold Spend Transaction hex**
6. **Copy the Hot Spend Transaction hex**

**Checkpoint**: Both transactions generated successfully.

#### Step 5: Verify Cold Spend Transaction

1. Navigate back → Tap **"Verify Vault Transaction"**
2. Paste the **Vault Context JSON** from Step 1
3. Paste the **Cold Spend Transaction hex** from Step 4
4. Tap **"Verify Transaction"**

**Checkpoint**: Verification shows:
- Transaction Type: `"ColdSpend"`
- Valid: `true`

#### Step 6: Verify Hot Spend Transaction

1. Stay on **"Verify Vault Transaction"** screen
2. Clear the transaction hex field
3. Paste the **Hot Spend Transaction hex** from Step 4
4. Tap **"Verify Transaction"**

**Checkpoint**: Verification shows:
- Transaction Type: `"HotSpend"`
- Valid: `true`

### Data Flow Verification

During the end-to-end test, verify that:

1. **Vault Context JSON** flows correctly between screens:
   - Created in Step 1
   - Used in Step 2 (unvaulting)
   - Updated context used in Step 4 (spending)
   - Original context used in verification steps

2. **Transaction IDs** are correctly referenced:
   - Unvaulting TXID from Step 2 used in Step 4
   - Transaction hex values are valid and consistent

3. **Transaction Relationships**:
   - Unvaulting transaction spends from vault address
   - Cold/Hot spend transactions spend from unvaulting transaction output
   - All transactions verify correctly with the original vault context

---

## Edge Cases and Error Scenarios

### Network Connectivity Issues

#### Test: Server Not Running

**Steps**:
1. Stop the Ktor server
2. Try to create a vault in the app

**Expected Results**:
- Error message displayed: Connection error or timeout
- Error message is user-friendly
- App doesn't crash

**Verification**:
- Error handling is graceful
- User can retry after starting server

#### Test: Wrong Server URL

**Steps**:
1. Modify API base URL to incorrect address
2. Try API operations

**Expected Results**:
- Connection errors displayed
- Clear error messages

### Input Validation Errors

#### Test: Invalid Address Formats

**Test Cases**:
- Mainnet address with testnet network
- Invalid character in address
- Too short/long address
- Empty address field

**Expected Results**:
- Button disabled or error shown before submission
- Clear validation messages
- No API call made with invalid data

#### Test: Invalid JSON Format

**Steps**:
1. In Unvault Funds screen, paste invalid JSON:
   - Missing closing brace
   - Invalid key names
   - Wrong data types
2. Try to submit

**Expected Results**:
- Error message about invalid JSON
- No transaction generated
- User can correct and retry

#### Test: Invalid Transaction Hex

**Steps**:
1. In Verify Transaction screen, paste:
   - Random hex string
   - Too short hex string
   - Non-hex characters
   - Empty string

**Expected Results**:
- Validation prevents submission or shows error
- Clear error message
- No verification attempted with invalid hex

### Boundary Conditions

#### Test: Very Large Amounts

**Steps**:
1. Create vault with very large amount (e.g., `2100000000000000` - max Bitcoin supply)

**Expected Results**:
- Vault created successfully (if within valid range)
- Amount correctly stored in JSON

#### Test: Very Small Amounts

**Steps**:
1. Create vault with small amount (e.g., `1000` satoshis)

**Expected Results**:
- Vault created successfully
- Note: Very small amounts may not be practical due to fees

#### Test: Maximum Block Delay

**Steps**:
1. Create vault with very large block delay (e.g., `65535` - max u16)

**Expected Results**:
- Vault created successfully
- Delay correctly stored in JSON

#### Test: Zero Block Delay

**Steps**:
1. Create vault with `0` block delay

**Expected Results**:
- Vault created (though not practical - no delay for hot spend)
- Delay value stored as `0` in JSON

### Transaction State Errors

#### Test: Mismatched Vault Context

**Steps**:
1. Create Vault A and Vault B
2. Create unvaulting transaction for Vault A
3. Try to verify with Vault B's context

**Expected Results**:
- Verification fails
- Error message indicates mismatch

#### Test: Invalid TXID/Vout Combination

**Steps**:
1. Use valid vault context
2. Use TXID that doesn't exist or wrong vout index

**Expected Results**:
- Transaction may be generated (structure valid)
- But transaction won't be spendable on actual network
- Note: App generates transaction structure, doesn't validate UTXO existence

### UI/UX Edge Cases

#### Test: Rapid Button Taps

**Steps**:
1. Rapidly tap "Create Vault" button multiple times

**Expected Results**:
- Only one request sent
- Loading state prevents duplicate submissions
- No duplicate results displayed

#### Test: Screen Rotation

**Steps**:
1. Fill in form fields
2. Rotate device
3. Verify data persistence

**Expected Results**:
- Form data retained after rotation
- No data loss
- UI adapts to new orientation

#### Test: Copy/Paste Operations

**Steps**:
1. Copy vault context JSON
2. Paste into different screens
3. Verify paste works correctly

**Expected Results**:
- Paste operation works
- Long JSON strings handled properly
- Text fields scrollable for long content

---

## Test Data

### Sample Testnet Addresses

Use these addresses for testing (testnet addresses are safe to use):

#### Legacy Addresses (P2PKH)
- `mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef`
- `n4eti5whH9GtLKABuJHFm226aSmb1kdARh`

#### Segwit Addresses (P2WPKH/P2WSH)
- `tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx`
- `tb1qrp33g0q5c5txsp9arysrx4k6zdkfs4nce4xj0gdcccefvpysxf3q0sl5k7`

#### Taproot Addresses (P2TR)
- `tb1pqqqqp399et2xygdj5xreqhjjvcmzhxw4ayxecm1zw5h6k24vq1fqjpyh5c`
- `tb1p5d7rjq7g6rdk2yhzks9smlaqtedr4dekq08ge8ztwac72sfr9rusxg3297`

### Example Vault Context JSON

#### Segwit Vault
```json
{
  "hot": "tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx",
  "cold": "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
  "amount": "1000000",
  "network": "testnet",
  "delay": 144,
  "taproot": false
}
```

#### Taproot Vault
```json
{
  "hot": "tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx",
  "cold": "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
  "amount": "1000000",
  "network": "testnet",
  "delay": 144,
  "taproot": true
}
```

### Expected Response Formats

#### Vaulting Response
```json
{
  "vault": "{\"hot\":\"...\",\"cold\":\"...\",\"amount\":\"1000000\",\"network\":\"testnet\",\"delay\":144,\"taproot\":false}",
  "address": "mgDrrGRPouPXEWhgzuwDXQZRovnNv1GF5x"
}
```

#### Unvaulting Response
```json
{
  "vault": "{\"hot\":\"...\",\"cold\":\"...\",\"amount\":\"1000000\",\"network\":\"testnet\",\"delay\":144,\"taproot\":false}",
  "script": "63029000b2751f01f427d7b0a6334728a5dc3b1b3d92f1e024ea7d9a430c41f29b1e33a4e59101dcb3671f43d591e156a317ac5010a620ef3c7001c53b459e60ba885fdbfbbe1bec34d201b7b368",
  "tx": "01000000000102...",
  "txid": "af20f98529569c929a0b0003e93519f73ebe8a20c6386838b690e516a6394929"
}
```

#### Spending Response
```json
{
  "coldTx": "01000000000101...",
  "hotTx": "02000000000101..."
}
```

#### Verification Response
```json
{
  "transactionType": "Unvaulting",
  "valid": true,
  "message": "Transaction is a valid unvaulting transaction"
}
```

### Test Transaction IDs

For testing purposes, you can use placeholder TXIDs (all zeros):
- `0000000000000000000000000000000000000000000000000000000000000000`

**Important**: These are placeholders for testing transaction structure. In production, use actual transaction IDs from the blockchain.

---

## Test Checklist

Use this checklist to ensure all functionality is tested:

### Vault Creation
- [ ] Create Segwit vault
- [ ] Create Taproot vault
- [ ] Test with different block delays
- [ ] Test with different networks
- [ ] Test input validation
- [ ] Test error handling

### Unvaulting
- [ ] Create unvaulting transaction
- [ ] Test with different vout values
- [ ] Test input validation
- [ ] Test error handling

### Spending
- [ ] Generate cold spend transaction
- [ ] Generate hot spend transaction
- [ ] Verify transaction differences
- [ ] Test input validation
- [ ] Test error handling

### Verification
- [ ] Verify unvaulting transaction
- [ ] Verify cold spend transaction
- [ ] Verify hot spend transaction
- [ ] Test with invalid transactions
- [ ] Test with mismatched vault context

### End-to-End
- [ ] Complete workflow test
- [ ] Data flow verification
- [ ] Transaction relationship verification

### Edge Cases
- [ ] Network connectivity issues
- [ ] Invalid input formats
- [ ] Boundary conditions
- [ ] UI/UX edge cases

---

## Troubleshooting

### Common Issues

#### App Can't Connect to Server

**Symptoms**: Error messages about connection failures

**Solutions**:
1. Verify server is running: `curl http://localhost:8080`
2. Check you're using emulator (for `10.0.2.2`) or correct IP for physical device
3. Verify firewall isn't blocking port 8080
4. Check server logs for errors

#### Invalid Address Errors

**Symptoms**: Error when creating vault with address

**Solutions**:
1. Ensure address matches selected network (testnet/mainnet)
2. Verify address format is correct
3. Use addresses from the Test Data section

#### JSON Parsing Errors

**Symptoms**: Error when pasting vault context JSON

**Solutions**:
1. Ensure complete JSON is copied (including all braces)
2. Verify no extra characters added
3. Check JSON is from the same vault operation

#### Transaction Verification Fails

**Symptoms**: Valid transactions show as invalid

**Solutions**:
1. Ensure vault context matches the transaction's vault
2. Verify transaction hex is complete and correct
3. Check transaction wasn't modified after generation

---

## Notes

- All test cases use **testnet** addresses for safety
- Transaction hex values are long - ensure complete copy/paste
- Vault Context JSON must be preserved between operations
- In production, use actual transaction IDs from the blockchain
- Block delays are in blocks, not time (144 blocks ≈ 1 day)
- Cold spend transactions can be broadcast immediately
- Hot spend transactions require waiting for block delay

---

## Additional Resources

- See `README.md` for server setup and API documentation
- See `VAULT_VERIFICATION_TEST_CASES.md` for API endpoint testing
- Android app source: `mobile/composeApp/src/commonMain/kotlin/com/gembotics/ctv/ui/screens/`
- Server source: `mobile/server/src/main/kotlin/com/gembotics/ctv/`
