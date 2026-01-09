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
