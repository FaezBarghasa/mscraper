# M-scraper (mscraper)

A multi‑platform cyberpunk-themed music player and download manager for Android and Linux built on a high-performance Rust core.

## Overview

**M-scraper** (developed as `mscraper`) integrates a high‑performance Rust download engine (`mm-dlp`) with a modern Kotlin/Compose Android front‑end. Featuring a unique cyberpunk aesthetic with glassmorphism effects and real-time visualizers, it provides a premium experience for managing and enjoying your music library.

- **High-Performance Rust Core** – Leverages Rust's async runtime and zero‑copy buffers for fast, reliable data processing.
- **Cyberpunk UI** – Beautifully crafted glassmorphism interface with neon accents, CRT scanline effects, and dynamic audio visualizers.
- **Unified Native Engine** – The `MmDlpEngine` wrapper allows the same core logic to run on both Android and Linux desktops.
- **Extensible Architecture** – Modular design allows for easy addition of new protocols or UI components.
- **Offline First** – Robust local database and settings management with built-in backup and restore support.

## Project Structure

```
mscraper/
├─ app/                     # Android application module (M-scraper UI)
│   ├─ src/main/java/com/example/ui/      # Compose UI, Themes, and Components
│   ├─ src/main/java/com/example/core/    # Native bridge and Logic
│   └─ src/main/res/xml/                  # Backup and Data rules
├─ mm-dlp/                  # Rust library (git submodule)
├─ LICENSE                  # MIT License details
├─ DISCLAIMER.md            # Usage and legal disclaimer
└─ README.md                # This file
```

## Prerequisites

- **Android**: Android Studio Jellyfish+, JDK 17+, Gradle 8.4+, Android device (API 26+).
- **Linux**: Recent distribution (Ubuntu 22.04+ recommended), JDK 17+.
- **Rust**: `rustup` with targets `aarch64-linux-android` and `x86_64-unknown-linux-gnu`.

## Building and Development

### Android App

1. Build the Rust core for Android:
   ```bash
   cd mm-dlp
   cargo build --release --target aarch64-linux-android
   cp target/aarch64-linux-android/release/libmm_dlp_core.so ../app/src/main/jniLibs/arm64-v8a/
   ```
2. Open the project in Android Studio or build via CLI:
   ```bash
   ./gradlew :app:assembleDebug
   ```

### Linux Desktop Demo

1. Build for Linux:
   ```bash
   cd mm-dlp
   cargo build --release --target x86_64-unknown-linux-gnu
   mkdir -p ../app/src/main/resources/linux
   cp target/x86_64-unknown-linux-gnu/release/libmm_dlp_core.so ../app/src/main/resources/linux/
   ```
2. Run the console demo:
   ```bash
   ./gradlew runLinuxDemo
   ```

## Documentation

- **UI Polish**: The interface uses customized `GlassCard` and `CRTEffect` components for its signature look.
- **Backup & Restore**: User settings and library data are handled via Android's `data-extraction-rules.xml`.
- **Legal**: Please review the [DISCLAIMER.md](DISCLAIMER.md) before use.

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for the full license text.

## Disclaimer

**Educational Use Only.** The authors are not responsible for any misuse of this tool. Users must comply with all applicable laws and terms of service. See [DISCLAIMER.md](DISCLAIMER.md) for details.

---
*Developed with ❤️ by the M-scraper Project Contributors.*
