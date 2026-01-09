# Bitcoinj 0.16.1 ArrayIndexOutOfBoundsException Bug Report

## Related Issues

This bug is related to [bitcoinj Issue #3439](https://github.com/bitcoinj/bitcoinj/issues/3439), which describes a similar `ArrayIndexOutOfBoundsException` when parsing raw transactions in bitcoinj 0.16.2.

## Issue Summary

When using bitcoinj 0.16.1, the `Utils.sha256hash160()` method throws an `ArrayIndexOutOfBoundsException: Index 32 out of bounds for length 32` when processing certain Bitcoin scripts, specifically scripts containing:
- OP_IF/OP_ELSE/OP_ENDIF conditional structures
- 32-byte data pushes (using OP_PUSHDATA1 or direct push opcodes)

This appears to be a broader issue in bitcoinj 0.16.x versions affecting array bounds checking in various parts of the library.

## Affected Code Paths

1. **VaultHelper.createUnvaultAddress()** - Creates P2WSH addresses from unvault redeem scripts
2. **CtvHelper.createCtvAddress()** - Creates P2WSH addresses from CTV locking scripts

## Error Details

```
java.lang.ArrayIndexOutOfBoundsException: Index 32 out of bounds for length 32
```

This error occurs when `Utils.sha256hash160()` processes script bytes that contain 32-byte CTV hashes within conditional script structures.

## Workarounds Implemented

### 1. BouncyCastle RIPEMD160 Dependency
Added `org.bouncycastle:bcprov-jdk18on:1.78.1` to provide RIPEMD160 support.

### 2. Manual hash160 Computation
Instead of using `Utils.sha256hash160()`, we now:
1. Compute SHA256 hash of script bytes manually
2. Compute RIPEMD160 hash of the SHA256 result using BouncyCastle

```kotlin
// Compute SHA256 hash of script (for P2WSH)
val sha256 = java.security.MessageDigest.getInstance("SHA-256")
val scriptHash = sha256.digest(redeemScriptBytes)

// Compute RIPEMD160(SHA256(script)) manually using BouncyCastle
val ripemd160 = MessageDigest.getInstance("RIPEMD160", "BC")
ripemd160.update(scriptHash)
val hash160 = ripemd160.digest()
```

### 3. Direct Script Byte Construction
Created `createUnvaultRedeemScriptBytes()` function that returns raw byte arrays instead of `Script` objects to avoid potential Script constructor bugs.

## Status

- ✅ **RESOLVED** - Upgraded to bitcoinj 0.17, which fixes the ArrayIndexOutOfBoundsException bug
- ✅ BouncyCastle dependency added (kept as fallback)
- ✅ Manual hash160 computation implemented (kept as fallback)
- ✅ Direct script byte construction implemented
- ✅ All endpoints tested and working correctly

## Recommendations

1. **Upgrade to bitcoinj 0.17** - This version has significant API changes but may fix the bug. Requires refactoring imports (classes moved to `org.bitcoinj.base` package). The bug appears to affect bitcoinj 0.16.x versions, so upgrading may resolve it.

2. **Reference existing issue** - This bug is related to [bitcoinj Issue #3439](https://github.com/bitcoinj/bitcoinj/issues/3439). Check that issue for any updates or fixes from the bitcoinj maintainers.

3. **Alternative workaround** - Consider using a different Bitcoin library or implementing a custom hash160 computation that completely bypasses bitcoinj's Utils class. Our current workaround using BouncyCastle is a step in this direction.

## Files Modified

- `mobile/server/build.gradle.kts` - Added BouncyCastle dependency
- `mobile/server/src/main/kotlin/com/gembotics/ctv/ctv/VaultHelper.kt` - Implemented manual hash160 computation
- `mobile/server/src/main/kotlin/com/gembotics/ctv/ctv/CtvHelper.kt` - Implemented manual hash160 computation

## Test Results

### Before Upgrade (bitcoinj 0.16.1)
- ✅ Vaulting endpoint: Works correctly
- ✅ Simple CTV locking endpoint: Works correctly  
- ❌ Unvaulting endpoint: Fails with ArrayIndexOutOfBoundsException

### After Upgrade (bitcoinj 0.17)
- ✅ Vaulting endpoint: Works correctly
- ✅ Unvaulting endpoint: **Now works correctly** - Bug fixed!
- ✅ Simple CTV locking endpoint: Works correctly

## Next Steps

1. Investigate if the error occurs in a different code path (e.g., Script constructor, transaction building)
2. Consider upgrading to bitcoinj 0.17 with proper migration
3. Report bug to bitcoinj project with minimal reproducible example
