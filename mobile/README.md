# CTV Playground - Mobile App

This directory contains the Android app and Ktor server implementation of the CTV Playground.

## Structure

- **`composeApp/`** - Android app using Jetpack Compose
- **`server/`** - Ktor server backend
- **`shared/`** - Shared code between Android and server (data models, API client)

## Prerequisites

- Android Studio or IntelliJ IDEA
- JDK 11 or higher
- Android SDK (API 24+)

## Running the Server

The Ktor server runs on port 8080 by default (configurable in `shared/src/commonMain/kotlin/com/gembotics/ctv/Constants.kt`).

### From Terminal

```bash
cd mobile
./gradlew :server:run
```

### From IDE

Run the `server` module with the main class `com.gembotics.ctv.ApplicationKt`.

## Running the Android App

### From Terminal

```bash
cd mobile
./gradlew :composeApp:assembleDebug
# Then install on device/emulator
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

### From Android Studio

1. Open the `mobile` directory in Android Studio
2. Select the `composeApp` run configuration
3. Click Run

## API Endpoints

The server provides the following endpoints:

### Simple CTV
- `GET /simple` - Simple CTV info
- `POST /simple/locking` - Create locking script
- `POST /simple/spending` - Create spending transaction

### Vaults
- `GET /vaults` - Vaults info
- `POST /vaults/vaulting` - Create vault
- `POST /vaults/unvaulting` - Unvault funds
- `POST /vaults/spending` - Spend from vault

## Configuration

### Server Port

Edit `shared/src/commonMain/kotlin/com/gembotics/ctv/Constants.kt` to change the server port.

### API Base URL (Android)

The Android app uses `http://10.0.2.2:8080` by default (Android emulator localhost).
For physical devices, update the base URL in `composeApp/src/commonMain/kotlin/com/gembotics/ctv/ui/App.kt` to your machine's IP address.

## Implementation Status

✅ **CTV functionality is fully implemented using bitcoinj!**

The server uses `bitcoinj` (version 0.17.0) to perform all CTV calculations:
- Simple CTV locking script generation
- Simple CTV spending transaction creation
- Vault creation and management
- Unvaulting transactions
- Cold and hot spend transactions

All CTV logic is implemented in:
- `server/src/main/kotlin/com/gembotics/ctv/ctv/CtvHelper.kt` - Core CTV operations
- `server/src/main/kotlin/com/gembotics/ctv/ctv/VaultHelper.kt` - Vault-specific operations
- `server/src/main/kotlin/com/gembotics/ctv/routes/` - API route handlers

## Development

### Adding New Features

1. Add data models in `shared/src/commonMain/kotlin/com/gembotics/ctv/models/`
2. Add API methods in `shared/src/commonMain/kotlin/com/gembotics/ctv/api/CtvApi.kt`
3. Add server routes in `server/src/main/kotlin/com/gembotics/ctv/routes/`
4. Add UI screens in `composeApp/src/commonMain/kotlin/com/gembotics/ctv/ui/screens/`
5. Update navigation in `composeApp/src/commonMain/kotlin/com/gembotics/ctv/ui/navigation/Navigation.kt`
