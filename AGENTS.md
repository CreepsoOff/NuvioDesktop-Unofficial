# Nuvio Mobile - Agent Instructions

## Cursor Cloud specific instructions

### Overview

Nuvio is a Kotlin Multiplatform (KMP) + Compose Multiplatform mobile app (Android + iOS). On Linux VMs, only the **Android target** can be fully built and tested. iOS Kotlin compilation (metadata/klib) works, but linking native frameworks requires macOS/Xcode.

### Environment

- **Android SDK** is installed at `/opt/android-sdk` (platforms;android-36, build-tools;36.0.0)
- **JDK 21** is pre-installed on the VM
- `local.properties` must exist at the project root with at least `sdk.dir=/opt/android-sdk`. If missing, create it.
- Environment variables needed in shell: `export ANDROID_HOME=/opt/android-sdk`

### Build Flavors

The project has two product flavors: `full` and `playstore`. The `playstore` flavor is the simpler one (no QuickJS plugin engine). Use `playstore` for standard CI checks.

### Key Commands

| Action | Command |
|--------|---------|
| Build debug APK | `./gradlew :composeApp:assemblePlaystoreDebug` |
| Run lint | `./gradlew :composeApp:lintPlaystoreDebug` |
| Run unit tests | `./gradlew :composeApp:testPlaystoreDebugUnitTest` |
| iOS Kotlin compile check | `./gradlew :composeApp:compileKotlinIosSimulatorArm64` |

### Gotchas

- **Lint will report pre-existing warnings/errors** (e.g., `MissingPermission`). These are known issues in the codebase and should not block PRs unless you introduce new lint errors.
- **`local.properties` is .gitignored**. It will not exist on fresh clones. The update script creates it if missing.
- **No emulator or device** is available in Cloud Agent VMs, so you cannot run the app on-device. Build verification (`assembleDebug`) is the extent of runtime validation available.
- **Gradle configuration cache** is enabled. If you change `build.gradle.kts` files and hit cache issues, add `--no-configuration-cache` or delete `.gradle/configuration-cache/`.
- **Version info** comes from `iosApp/Configuration/Version.xcconfig` — this is the single source of truth for both platforms.
- The `full` flavor requires additional `.aar` files in `composeApp/libs/` (QuickJS). The `playstore` flavor builds without them.
