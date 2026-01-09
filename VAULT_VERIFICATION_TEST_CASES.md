# Vault Verification API Test Cases

This document provides comprehensive test cases for the CTV Vault Verification API endpoints.

## Prerequisites

1. **Start the server**:
   ```bash
   cd mobile
   ./gradlew :server:run
   ```

2. **Verify server is running**:
   The server should be accessible at `http://localhost:8080`

---

## Test Case 1: Vaulting Endpoint

Creates a new vault and returns the vault address.

### Request

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

### Expected Response

```json
{
    "vault": "{\"hotAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"coldAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"amount\":\"1000000\",\"network\":\"testnet\",\"blockDelay\":144,\"taproot\":false}",
    "address": "mgDrrGRPouPXEWhgzuwDXQZRovnNv1GF5x"
}
```

**Note**: Save the `vault` JSON value for use in subsequent tests.

---

## Test Case 2: Unvaulting Endpoint

Creates an unvaulting transaction that moves funds from the vault to an unvault address.

### Request

```bash
curl -X POST http://localhost:8080/vaults/unvaulting \
  -H "Content-Type: application/json" \
  -d '{
    "vault": "{\"hotAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"coldAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"amount\":\"1000000\",\"network\":\"testnet\",\"blockDelay\":144,\"taproot\":false}",
    "txid": "0000000000000000000000000000000000000000000000000000000000000000",
    "vout": 0
  }' | python3 -m json.tool
```

### Expected Response

```json
{
    "vault": "{\"hotAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"coldAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"amount\":\"1000000\",\"network\":\"testnet\",\"blockDelay\":144,\"taproot\":false}",
    "script": "63029000b2751f01f427d7b0a6334728a5dc3b1b3d92f1e024ea7d9a430c41f29b1e33a4e59101dcb3671f43d591e156a317ac5010a620ef3c7001c53b459e60ba885fdbfbbe1bec34d201b7b368",
    "tx": "0100000000010100000000000000000000000000000000000000000000000000000000000000000000000021002058626fd9e17eefcd16f7499f0b2687ba359692838b702e185c144d894ae3e4ffffffff01e83f0f00000000001976a914432c6152a179b6988757eaf7733693e1bceb425888ac02203a4074350970f6314f6c95fa881a55ddf51c68768c8c3d9033db8016e02eb3f6234c203a4074350970f6314f6c95fa881a55ddf51c68768c8c3d9033db8016e02eb3f6b300000000",
    "txid": "36f367f9be45293741d9d4622325dffec5cce38607e76f7778c7c2c7947f493f"
}
```

**Note**: Save the `tx` (transaction hex) value for Test Case 3.

---

## Test Case 3: Vault Verification Endpoint (Unvaulting)

Verifies that an unvaulting transaction is valid for the given vault context.

### Request

```bash
curl -X POST http://localhost:8080/vaults/verification \
  -H "Content-Type: application/json" \
  -d '{
    "vault": "{\"hotAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"coldAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"amount\":\"1000000\",\"network\":\"testnet\",\"blockDelay\":144,\"taproot\":false}",
    "tx": "0100000000010100000000000000000000000000000000000000000000000000000000000000000000000021002058626fd9e17eefcd16f7499f0b2687ba359692838b702e185c144d894ae3e4ffffffff01e83f0f00000000001976a914432c6152a179b6988757eaf7733693e1bceb425888ac02203a4074350970f6314f6c95fa881a55ddf51c68768c8c3d9033db8016e02eb3f6234c203a4074350970f6314f6c95fa881a55ddf51c68768c8c3d9033db8016e02eb3f6b300000000"
  }' | python3 -m json.tool
```

### Expected Response

```json
{
    "transactionType": "Unvaulting",
    "valid": true,
    "message": "Transaction is a valid unvaulting transaction"
}
```

---

## Test Case 4: Simple CTV Locking Endpoint

Creates a simple CTV locking script.

### Request

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

### Expected Response

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

## Test Case 5: Vault Spending Endpoint

Creates spending transactions (cold and hot) from an unvault transaction.

### Request

```bash
curl -X POST http://localhost:8080/vaults/spending \
  -H "Content-Type: application/json" \
  -d '{
    "vault": "{\"hotAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"coldAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"amount\":\"1000000\",\"network\":\"testnet\",\"blockDelay\":144,\"taproot\":false}",
    "txid": "36f367f9be45293741d9d4622325dffec5cce38607e76f7778c7c2c7947f493f",
    "type": "cold"
  }' | python3 -m json.tool
```

### Expected Response

```json
{
    "coldTx": "010000000001013f497f94c7c2c778776fe70786e3ccc5fedf252362d4d941372945bef967f3360000000021001af97c837d705648eb2cf72cdc865ae8a718276058f49870d7524d6fdfa20480ffffffff01903d0f00000000001976a914ccc198c15d8344c73da67a75509a85a8f422663688ac02004e63029000b2754c2001f427d7b0a6334728a5dc3b1b3d92f1e024ea7d9a430c41f29b1e33a4e591dcb3674c2043d591e156a317ac5010a620ef3c7001c53b459e60ba885fdbfbbe1bec34d2b7b36800000000",
    "hotTx": "010000000001013f497f94c7c2c778776fe70786e3ccc5fedf252362d4d941372945bef967f3360000000021001af97c837d705648eb2cf72cdc865ae8a718276058f49870d7524d6fdfa20480ffffffff01903d0f00000000001976a914ccc198c15d8344c73da67a75509a85a8f422663688ac0201014e63029000b2754c2001f427d7b0a6334728a5dc3b1b3d92f1e024ea7d9a430c41f29b1e33a4e591dcb3674c2043d591e156a317ac5010a620ef3c7001c53b459e60ba885fdbfbbe1bec34d2b7b36800000000"
}
```

**Note**: The response includes both `coldTx` (immediate spend) and `hotTx` (delayed spend) transactions.

---

## Test Case 6: Verify Cold Spend Transaction

Verifies that a cold spend transaction is valid.

### Request

```bash
curl -X POST http://localhost:8080/vaults/verification \
  -H "Content-Type: application/json" \
  -d '{
    "vault": "{\"hotAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"coldAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"amount\":\"1000000\",\"network\":\"testnet\",\"blockDelay\":144,\"taproot\":false}",
    "tx": "010000000001013f497f94c7c2c778776fe70786e3ccc5fedf252362d4d941372945bef967f3360000000021001af97c837d705648eb2cf72cdc865ae8a718276058f49870d7524d6fdfa20480ffffffff01903d0f00000000001976a914ccc198c15d8344c73da67a75509a85a8f422663688ac02004e63029000b2754c2001f427d7b0a6334728a5dc3b1b3d92f1e024ea7d9a430c41f29b1e33a4e591dcb3674c2043d591e156a317ac5010a620ef3c7001c53b459e60ba885fdbfbbe1bec34d2b7b36800000000"
  }' | python3 -m json.tool
```

### Expected Response

```json
{
    "transactionType": "ColdSpend",
    "valid": true,
    "message": "Transaction is a valid cold spend transaction"
}
```

---

## Test Case 7: Verify Hot Spend Transaction

Verifies that a hot spend transaction is valid.

### Request

```bash
curl -X POST http://localhost:8080/vaults/verification \
  -H "Content-Type: application/json" \
  -d '{
    "vault": "{\"hotAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"coldAddress\":\"mzBc4XEFSdzCDcTxAgf6EZXgsZWpztRhef\",\"amount\":\"1000000\",\"network\":\"testnet\",\"blockDelay\":144,\"taproot\":false}",
    "tx": "010000000001013f497f94c7c2c778776fe70786e3ccc5fedf252362d4d941372945bef967f3360000000021001af97c837d705648eb2cf72cdc865ae8a718276058f49870d7524d6fdfa20480ffffffff01903d0f00000000001976a914ccc198c15d8344c73da67a75509a85a8f422663688ac0201014e63029000b2754c2001f427d7b0a6334728a5dc3b1b3d92f1e024ea7d9a430c41f29b1e33a4e591dcb3674c2043d591e156a317ac5010a620ef3c7001c53b459e60ba885fdbfbbe1bec34d2b7b36800000000"
  }' | python3 -m json.tool
```

### Expected Response

```json
{
    "transactionType": "HotSpend",
    "valid": true,
    "message": "Transaction is a valid hot spend transaction"
}
```

---

## Automated Test Script

For convenience, here's a complete automated test script that runs all tests sequentially:

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

echo "=== Test 3: Vault Verification (Unvaulting) ==="
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
echo ""

UNVAULT_TXID=$(echo $UNVAULT_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['txid'])")

echo "=== Test 5: Vault Spending ==="
SPEND_RESPONSE=$(curl -s -X POST http://localhost:8080/vaults/spending \
  -H "Content-Type: application/json" \
  -d "{
    \"vault\": $VAULT_JSON,
    \"txid\": \"$UNVAULT_TXID\",
    \"type\": \"cold\"
  }")
echo "$SPEND_RESPONSE" | python3 -m json.tool
echo ""

COLD_TX=$(echo $SPEND_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['coldTx'])")
HOT_TX=$(echo $SPEND_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['hotTx'])")

echo "=== Test 6: Verify Cold Spend ==="
curl -s -X POST http://localhost:8080/vaults/verification \
  -H "Content-Type: application/json" \
  -d "{
    \"vault\": $VAULT_JSON,
    \"tx\": \"$COLD_TX\"
  }" | python3 -m json.tool
echo ""

echo "=== Test 7: Verify Hot Spend ==="
curl -s -X POST http://localhost:8080/vaults/verification \
  -H "Content-Type: application/json" \
  -d "{
    \"vault\": $VAULT_JSON,
    \"tx\": \"$HOT_TX\"
  }" | python3 -m json.tool
```

Save this as `test_all_endpoints.sh`, make it executable, and run:

```bash
chmod +x test_all_endpoints.sh
./test_all_endpoints.sh
```

---

## Test Results Summary

All tests have been verified and pass successfully:

- ✅ **Test 1**: Vaulting Endpoint - Creates vault successfully
- ✅ **Test 2**: Unvaulting Endpoint - Creates unvault transaction successfully
- ✅ **Test 3**: Vault Verification (Unvaulting) - Verifies unvaulting transaction correctly
- ✅ **Test 4**: Simple CTV Locking - Creates CTV lock successfully
- ✅ **Test 5**: Vault Spending Endpoint - Creates spending transactions successfully
- ✅ **Test 6**: Vault Verification (Cold Spend) - Verifies cold spend transaction correctly
- ✅ **Test 7**: Vault Verification (Hot Spend) - Verifies hot spend transaction correctly

---

## Transaction Types Supported by Verification

The verification endpoint can identify and validate three types of vault transactions:

1. **Unvaulting** - Transaction that moves funds from vault to unvault address
2. **ColdSpend** - Immediate spend transaction (no delay required)
3. **HotSpend** - Delayed spend transaction (requires block delay)

---

## Notes

- All endpoints use **testnet** addresses for testing
- Transaction hex values can be used with Bitcoin testnet explorers or testnet nodes
- The verification endpoint validates transaction structure, witness data, and script compatibility
- All endpoints return JSON responses with appropriate error handling
