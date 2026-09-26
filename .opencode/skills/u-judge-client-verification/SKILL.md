---
name: u-judge-client-verification
description: Use when testing, releasing, packaging, or changing Gradle, dependencies, Android builds, iOS builds, or client toolchain files.
---

# U'Judge Client Verification

## Delivery scope

- Before implementation, name the shared increment (`I1`...`I8` in `docs/ROADMAP.md`) and the requirement IDs or gate
  items it will close. Use the `u-judge-client-increment-planning` skill.
- Include every client-owned layer required to prove that outcome: state, durable storage/outbox, transport contract, UI feedback, and Android/iPhone evidence when applicable.
- For a cross-repository outcome, link the server issue and PR, run the scenario against `./gradlew :desktop:run` from
  `u-judge-server`, and record the integration or physical-device evidence needed to close it.
- A narrow prerequisite is allowed only for an urgent fix, blocking preparation, CI/docs change, or independently useful dependency. Explain the exception and the parent outcome in the issue and PR; never mark partial evidence as a completed requirement or gate.

## Required checks

- Use JDK 21.
- For client source or dependency changes, run `./gradlew :androidApp:assembleDebug`.
- Verify shared/iOS compilation with `./gradlew :composeApp:compileKotlinIosArm64 :composeApp:compileKotlinIosSimulatorArm64`.
- Run `./gradlew :composeApp:testAndroidHostTest` after shared model changes.
- Run `./gradlew :composeApp:iosSimulatorArm64Test` when iOS simulator testing is available.
- For transport or combat changes, run the increment scenario on an Android emulator against a real server and record it in the PR.
- Run `git diff --check` before committing.

## Toolchain rules

- Compose 1.12 requires `compileSdk 37`; keep AGP at the highest version supported by the installed Android Studio, currently 9.2.1.
- `iosX64` is intentionally not configured because current Compose Multiplatform artifacts do not publish an Intel simulator variant.
- Do not add Android-only libraries to `commonMain`.
- Do not commit build directories, `.idea`, Xcode user data, credentials, or generated APKs unless explicitly requested.

## Release evidence

The pilot requires physical Android APK and iPhone TestFlight smoke tests in the target LAN, including Local Network permission, discovery, pairing, reconnect, and clean install.
