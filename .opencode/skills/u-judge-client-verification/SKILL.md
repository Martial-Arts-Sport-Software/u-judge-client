---
name: u-judge-client-verification
description: Use when testing, releasing, packaging, or changing Gradle, dependencies, Android builds, iOS builds, or client toolchain files.
---

# U'Judge Client Verification

## Required checks

- Use JDK 21.
- For client source or dependency changes, run `./gradlew :androidApp:assembleDebug`.
- Verify shared/iOS compilation with `./gradlew :composeApp:compileKotlinIosArm64 :composeApp:compileKotlinIosSimulatorArm64`.
- Run `./gradlew :composeApp:testAndroidHostTest` after shared model changes.
- Run `./gradlew :composeApp:iosSimulatorArm64Test` when iOS simulator testing is available.
- Run `git diff --check` before committing.

## Toolchain rules

- Compose 1.12 requires `compileSdk 37`; keep AGP at the highest version supported by the installed Android Studio, currently 9.2.1.
- `iosX64` is intentionally not configured because current Compose Multiplatform artifacts do not publish an Intel simulator variant.
- Do not add Android-only libraries to `commonMain`.
- Do not commit build directories, `.idea`, Xcode user data, credentials, or generated APKs unless explicitly requested.

## Release evidence

The pilot requires physical Android APK and iPhone TestFlight smoke tests in the target LAN, including Local Network permission, discovery, pairing, reconnect, and clean install.
