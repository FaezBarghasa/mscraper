# mscraper

A multi‑platform download manager for Android and Linux built on the **mm‑dlp** Rust core.

## Overview

`mscraper` integrates the high‑performance `mm-dlp` download engine (written in Rust) with a Kotlin/Android front‑end via **JNA**.  The same native library can also be used on Linux desktops, making the download manager truly cross‑platform.

- **Fast, reliable downloads** – leverages Rust's async runtime and zero‑copy buffers.
- **Unified API** – the Kotlin `MmDlpEngine` wrapper exposes the same methods on Android and Linux.
- **Progress tracking** – state flows are exposed to the UI for smooth progress animations.
- **Extensible** – you can add additional download protocols by extending the Rust core.

## Project Structure

```
mscraper/
├─ app/                     # Android application module
│   ├─ src/main/kotlin/com/example/core/
│   │   ├─ DownloadManager.kt   # Orchestrates UI ↔ native engine
│   │   ├─ MmDlpEngine.kt       # JNA wrapper around the Rust library
│   │   └─ NativeLibLoader.kt   # Loads the native .so on Android or Linux
│   └─ src/main/jniLibs/arm64-v8a/libmm_dlp_core.so   # Pre‑built Android library
├─ mm-dlp/                  # Rust library (git submodule)
│   └─ ...
└─ README.md                # *this file*
```

## Prerequisites

- **Android**: Android Studio, JDK 11+, Gradle 8+, an Android device or emulator.
- **Linux**: A recent Linux distribution (x86_64 or aarch64), JDK 11+, Gradle 8+.
- **Rust toolchain**: `rustup` with the targets `aarch64-linux-android` (for Android) and `x86_64-unknown-linux-gnu` (for Linux).

## Building for Android

```bash
# Build the Rust library for Android
cd mm-dlp
cargo build --release --target aarch64-linux-android
# Copy the generated .so into the Android module
cp target/aarch64-linux-android/release/libmm_dlp_core.so ../mscraper/app/src/main/jniLibs/arm64-v8a/

# Build the Android app
cd ../mscraper/app
./gradlew assembleDebug   # or assembleRelease for a signed APK
```

The app can now be installed on a device or launched from Android Studio.

## Building for Linux (desktop)

1. **Build the native library**
   ```bash
   cd mm-dlp
   cargo build --release --target x86_64-unknown-linux-gnu
   # The output is libmm_dlp_core.so in target/x86_64-unknown-linux-gnu/release/
   ```
2. **Package the library with the Kotlin demo**
   ```bash
   # Copy the .so to resources so the loader can find it at runtime
   mkdir -p ../mscraper/app/src/main/resources/linux
   cp target/x86_64-unknown-linux-gnu/release/libmm_dlp_core.so ../mscraper/app/src/main/resources/linux/
   ```
3. **Run the Linux demo (console application)**
   ```bash
   cd ../mscraper/app
   ./gradlew runLinuxDemo   # a custom Gradle task defined in build.gradle.kts
   ```
   The demo will start a small download and print progress to the console.

## License

The project is licensed under the **MIT License** – see the `LICENSE` file for the full text.

## Disclaimer

The software is provided **as‑is**, without any warranties of any kind, express or implied. The authors are not liable for any damages arising from the use of this software. Use at your own risk.

---
*For more detailed contribution guidelines and architecture diagrams, refer to the project wiki.*
