---
name: u-judge-client-compose
description: Use when changing U'Judge Kotlin Multiplatform Compose UI, navigation, localization, Android/iOS targets, or lifecycle state.
---

# U'Judge Client Compose

## Platform and UI rules

- Keep shared UI in `composeApp`; place Android-only dependencies and code in `androidMain`.
- Support Android and iPhone in landscape. Verify critical flows on physical pilot devices, not only the simulator.
- Preserve the existing design system and provide Russian and English resources for every user-facing string.
- Give combat controls and icons semantic labels; statuses must not depend on color alone.

## State rules

- Keep navigation separate from connection, pairing, session, and rating-draft state.
- Avoid bare global booleans for transport truth. Model lifecycle jobs so leaving a screen cancels pending discovery or actions.
- Persist only meaningful user state: identity, locale, settings, drafts, and pending events.
- Do not implement empty click handlers for declared pilot flows. Either deliver the behavior or visibly mark it unavailable.

## Verification

Run `./gradlew :androidApp:assembleDebug :composeApp:compileKotlinIosArm64 :composeApp:compileKotlinIosSimulatorArm64` after multiplatform changes. Run Android host tests and iOS simulator tests when changing shared models.
