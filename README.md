# CTV Playground

This is where I'm playing with OP_CTV things in Bitcoin

## TODO

- [ ] Better output entry
- [ ] Add decoy output to generate a fake spending transaction
- [x] Simple CTV vault

## Development

### Rust Server (Web UI)

#### Prerequisites

This project requires Rust and Cargo. If you don't have them installed:

**Install Rust using rustup** (recommended):
```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
```

After installation, restart your terminal or run:
```bash
source ~/.zshrc
```

**Verify installation**:
```bash
cargo --version
```

#### Running Locally

1. **Build the project**:
   ```bash
   cargo build
   ```

2. **Run the server**:
   ```bash
   cargo run
   ```

3. **Access the application**:
   Open your browser and navigate to `http://localhost:3000`

#### Development Tools

Run `npm install` to install the `package.json` node modules. It has the jinja2 prettier plugin to format the templates.

---

## Mobile Application (Kotlin Multiplatform with Ktor Server)

This project includes a Kotlin Multiplatform Android application with an embedded Ktor server that implements the CTV Playground logic using the `bitcoinj` library.

### Prerequisites

- **Android Studio** (latest version recommended)
- **JDK 17+** (Java Development Kit)
- **Android SDK** (installed via Android Studio)
- **Android Emulator** or physical Android device for testing

### Project Structure

- `mobile/composeApp` - Compose Multiplatform UI (Android)
- `mobile/server` - Ktor server application implementing CTV logic with `bitcoinj`
- `mobile/shared` - Shared code between Android app and Ktor server (data models, API client)

### Running the Ktor Server

1. **Navigate to the mobile directory**:
   ```bash
   cd mobile
   ```

2. **Run the Ktor server**:
   ```bash
   ./gradlew :server:run
   ```

   The server will start on `http://0.0.0.0:8080` (accessible at `http://localhost:8080` on your machine).

   **Note**: Keep this server running while testing the Android app.

### Running the Android Application

1. **Open the project in Android Studio**:
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to and select the `mobile` directory

2. **Set up Android Emulator** (if not already set up):
   - Go to Tools → Device Manager
   - Create a new virtual device (recommended: Pixel 5 or newer with API 33+)
   - Start the emulator

3. **Run the app**:
   - Select the `composeApp` configuration from the run configurations dropdown
   - Click the Run button (or press `Shift+F10`)
   - The app will build and install on your emulator/device

   **Important**: The Android app connects to the Ktor server at `http://10.0.2.2:8080` (this is the Android emulator's special IP address that maps to `localhost` on your host machine).

### Testing the Application

#### Testing Simple CTV Workflow

1. **Create a Locking Script**:
   - Open the app and tap "Simple CTV"
   - Tap "Create Locking Script"
   - Enter outputs in the format: `address:amount:data` (one per line)
     - Example for testnet: `tb1qw508d6qejxtdg4y5r3zarvary0c5xw7kxpjzsx:100000:`
   - Select network (testnet recommended for testing)
   - Optionally enable "Congestion Control" or "Taproot"
   - Tap "Create Locking Script"
   - **Copy the CTV Context JSON** from the result

2. **Create a Spending Transaction**:
   - Navigate back and tap "Create Spending Transaction"
   - Paste the CTV Context JSON from step 1
   - Enter the Transaction ID (TXID) of the funding transaction
   - Enter the Output Index (Vout) of the funding transaction
   - Tap "Create Spending Transaction"
   - The app will display the hex-encoded spending transaction

#### Testing CTV Vault Workflow

1. **Create a Vault**:
   - Open the app and tap "CTV Vaults"
   - Tap "Create Vault"
   - Enter:
     - Amount (in satoshis, e.g., `1000000`)
     - Cold Address (cold storage address)
     - Hot Address (hot wallet address)
     - Block Delay (e.g., `144` for ~1 day)
     - Network (testnet recommended)
   - Optionally enable "Taproot"
   - Tap "Create Vault"
   - **Copy the Vault Context JSON** from the result

2. **Unvault Funds**:
   - Navigate back and tap "Unvault Funds"
   - Paste the Vault Context JSON from step 1
   - Enter the Transaction ID (TXID) of the funding transaction
   - Enter the Output Index (Vout) of the funding transaction
   - Tap "Create Unvaulting Transaction"
   - **Copy the Updated Vault Context JSON** from the result

3. **Spend from Vault**:
   - Navigate back and tap "Spend from Vault"
   - Paste the Updated Vault Context JSON from step 2
   - Enter the Unvaulting Transaction ID (TXID from step 2)
   - Tap "Create Spending Transactions"
   - The app will display both:
     - Cold Spend Transaction (immediate, no delay)
     - Hot Spend Transaction (requires block delay)

### Troubleshooting

#### Server Won't Start (Port Already in Use)

If you see `Address already in use` error:

```bash
# Find and kill the process using port 8080
lsof -ti:8080 | xargs kill -9

# Then try running the server again
cd mobile && ./gradlew :server:run
```

#### Android App Can't Connect to Server

1. **Verify server is running**: Check that the Ktor server is running on port 8080
2. **Check emulator network**: Ensure you're using an Android emulator (not a physical device) - the app uses `10.0.2.2:8080` which only works on emulators
3. **For physical devices**: You'll need to change the API base URL in the app code to use your computer's local IP address (e.g., `192.168.1.x:8080`)

#### Build Errors

If you encounter Gradle build errors:

1. **Clean the build**:
   ```bash
   cd mobile
   ./gradlew clean
   ```

2. **Invalidate caches in Android Studio**:
   - File → Invalidate Caches → Invalidate and Restart

3. **Sync Gradle**:
   - File → Sync Project with Gradle Files

#### Address Validation Errors

- **Testnet addresses**: Use addresses starting with `tb1...`, `m...`, `n...`, or `2...`
- **Mainnet addresses**: Use addresses starting with `bc1...`, `1...`, or `3...`
- Ensure the network selection matches your address format

#### Missing Dependencies

If you see dependency resolution errors:

```bash
cd mobile
./gradlew --refresh-dependencies
```

---

## Testing

### API Endpoint Testing

The Ktor server provides REST API endpoints that can be tested using `curl` or any HTTP client. All endpoints return JSON responses.

#### Prerequisites

1. **Start the server**:
   ```bash
   cd mobile
   ./gradlew :server:run
   ```

2. **Verify server is running**:
   The server should be accessible at `http://localhost:8080`

#### Test Cases

##### Test 1: Vaulting Endpoint

Creates a new vault and returns the vault address.

```bash
curl -X POST http://localhost:8080/vaults/vaulting \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "1000000",
    "coldAddress": "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
    "hotAddress": "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
    "blockDelay": 144,
    "network": "testnet",
    "taproot": false
  }' | python3 -m json.tool
```

**Expected Response**:
```json
{
    "vault": "{\"hotAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"coldAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"amount\":\"1000000\",\"network\":\"testnet\",\"blockDelay\":144,\"taproot\":false}",
    "address": "mgDrrGRPouPXEWhgzuwDXQZRovnNv1GF5x"
}
```

**Save the `vault` JSON value for the next test.**

---

##### Test 2: Unvaulting Endpoint

Creates an unvaulting transaction that moves funds from the vault to an unvault address.

```bash
# Replace YOUR_VAULT_JSON with the vault JSON from Test 1
curl -X POST http://localhost:8080/vaults/unvaulting \
  -H "Content-Type: application/json" \
  -d '{
    "vault": YOUR_VAULT_JSON,
    "txid": "0000000000000000000000000000000000000000000000000000000000000000",
    "vout": 0
  }' | python3 -m json.tool
```

**Expected Response**:
```json
{
    "vault": "...",
    "script": "63029000b2751f01f427d7b0a6334728a5dc3b1b3d92f1e024ea7d9a430c41f29b1e33a4e59101dcb3671f43d591e156a317ac5010a620ef3c7001c53b459e60ba885fdbfbbe1bec34d201b7b368",
    "tx": "01000000000102...",
    "txid": "af20f98529569c929a0b0003e93519f73ebe8a20c6386838b690e516a6394929"
}
```

**Save the `tx` (transaction hex) value for Test 3.**

---

##### Test 3: Vault Verification Endpoint

Verifies that a transaction is valid for the given vault context. This is the main verification feature.

```bash
# Replace YOUR_VAULT_JSON and TRANSACTION_HEX with values from Tests 1 and 2
curl -X POST http://localhost:8080/vaults/verification \
  -H "Content-Type: application/json" \
  -d '{
    "vault": YOUR_VAULT_JSON,
    "tx": "TRANSACTION_HEX"
  }' | python3 -m json.tool
```

**Expected Response** (for unvaulting transaction):
```json
{
    "transactionType": "Unvaulting",
    "valid": true,
    "message": "Transaction is a valid unvaulting transaction"
}
```

**Possible Transaction Types**:
- `"Unvaulting"` - Transaction that moves funds from vault to unvault address
- `"ColdSpend"` - Immediate spend transaction (no delay)
- `"HotSpend"` - Delayed spend transaction (requires block delay)

---

##### Test 4: Simple CTV Locking Endpoint

Creates a simple CTV locking script.

```bash
curl -X POST http://localhost:8080/simple/locking \
  -H "Content-Type: application/json" \
  -d '{
    "outputs": "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef:100000:",
    "network": "testnet",
    "congestion": false,
    "taproot": false
  }' | python3 -m json.tool
```

**Expected Response**:
```json
{
    "ctvHash": "3adad25eb3017fef93fef9613c1f1f3b93d32b680f2ba4946dab8a48abed1b2a",
    "lockingScript": "PUSHDATA1[3adad25eb3017fef93fef9613c1f1f3b93d32b680f2ba4946dab8a48abed1b2a] NOP4",
    "lockingHex": "4c203adad25eb3017fef93fef9613c1f1f3b93d32b680f2ba4946dab8a48abed1b2ab3",
    "address": "n4eti5whH9GtLKABuJHFm226aSmb1kdARh",
    "ctv": "{\"network\":\"testnet\",\"version\":1,\"locktime\":0,\"sequences\":[0],\"inputIndex\":0,\"ctvHash\":\"3adad25eb3017fef93fef9613c1f1f3b93d32b680f2ba4946dab8a48abed1b2a\",\"outputs\":[{\"amount\":99400,\"scriptPubKey\":\"76a914ccc198c15d8344c73da67a75509a85a8f422663688ac\"}]}"
}
```

---

##### Test 5: Vault Spending Endpoint

Creates spending transactions (cold and hot) from an unvault transaction.

```bash
# First, get vault and unvault transaction (from Tests 1 and 2)
# Then test spending:
curl -X POST http://localhost:8080/vaults/spending \
  -H "Content-Type: application/json" \
  -d '{
    "vault": YOUR_VAULT_JSON,
    "txid": "UNVAULT_TXID_FROM_TEST_2",
    "type": "cold"
  }' | python3 -m json.tool
```

**Expected Response** (for cold spend):
```json
{
    "vault": "...",
    "coldTx": "01000000000101...",
    "hotTx": "01000000000101..."
}
```

---

### Automated Test Script

Save this as `test_all_endpoints.sh`:

```bash
#!/bin/bash

echo "=== Test 1: Vaulting ==="
VAULT_RESPONSE=$(curl -s -X POST http://localhost:8080/vaults/vaulting \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "1000000",
    "coldAddress": "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
    "hotAddress": "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef",
    "blockDelay": 144,
    "network": "testnet",
    "taproot": false
  }')
echo "$VAULT_RESPONSE" | python3 -m json.tool
echo ""

VAULT_JSON=$(echo $VAULT_RESPONSE | python3 -c "import sys, json; print(json.dumps(json.load(sys.stdin)['vault']))")

echo "=== Test 2: Unvaulting ==="
UNVAULT_RESPONSE=$(curl -s -X POST http://localhost:8080/vaults/unvaulting \
  -H "Content-Type: application/json" \
  -d "{
    \"vault\": $VAULT_JSON,
    \"txid\": \"0000000000000000000000000000000000000000000000000000000000000000\",
    \"vout\": 0
  }")
echo "$UNVAULT_RESPONSE" | python3 -m json.tool
echo ""

TX_HEX=$(echo $UNVAULT_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['tx'])")

echo "=== Test 3: Vault Verification ==="
curl -s -X POST http://localhost:8080/vaults/verification \
  -H "Content-Type: application/json" \
  -d "{
    \"vault\": $VAULT_JSON,
    \"tx\": \"$TX_HEX\"
  }" | python3 -m json.tool
echo ""

echo "=== Test 4: Simple CTV Locking ==="
curl -s -X POST http://localhost:8080/simple/locking \
  -H "Content-Type: application/json" \
  -d '{
    "outputs": "mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef:100000:",
    "network": "testnet",
    "congestion": false,
    "taproot": false
  }' | python3 -m json.tool
```

Make it executable and run:
```bash
chmod +x test_all_endpoints.sh
./test_all_endpoints.sh
```

---

### Expected Test Results

All tests should pass with the following results:

- ✅ **Vaulting**: Returns vault JSON and address
- ✅ **Unvaulting**: Returns transaction hex and txid
- ✅ **Verification**: Returns `{"transactionType": "Unvaulting", "valid": true, ...}`
- ✅ **Simple CTV**: Returns CTV hash and address
- ✅ **Spending**: Returns cold and hot spend transactions

### Notes

- All endpoints use **testnet** addresses for testing
- The verification endpoint can verify **Unvaulting**, **ColdSpend**, and **HotSpend** transaction types
- Transaction hex values can be used with Bitcoin testnet explorers or testnet nodes
