# AGENTS.md

## Cursor Cloud specific instructions

### Project Overview

Nuvio is a Kotlin Multiplatform (KMP) + Compose Multiplatform media streaming client. Targets: Android, iOS, Desktop (Windows/macOS/Linux). Single Gradle module `:composeApp` with a `mediamp` git submodule for the MPV video player integration.

### Build Requirements

- **JDK 21** (already available in the VM)
- **Android SDK** at `/opt/android-sdk` (compileSdk 36, minSdk 24)
- **mediamp submodule** must be initialized: `git submodule update --init mediamp`
- **Native build tools**: `cmake`, `ninja-build`, `g++`, `libmpv-dev`, `libgl-dev`
- **Mesa/X11 libraries**: `libgl1`, `libegl1`, `mesa-utils`, and X11 libs

### Configuration

`local.properties` must exist at the project root with:
```
sdk.dir=/opt/android-sdk
SUPABASE_URL=<from env>
SUPABASE_ANON_KEY=<from env>
TRAKT_CLIENT_ID=<from env>
TRAKT_CLIENT_SECRET=<from env>
TRAKT_REDIRECT_URI=<from env>
```

Secrets are available as environment variables. The Gradle build reads them from `local.properties` or environment variables (see `GenerateRuntimeConfigsTask` in `composeApp/build.gradle.kts`).

### Key Commands

| Task | Command |
|------|---------|
| Compile Desktop | `./gradlew :composeApp:compileKotlinDesktop -PCMAKE_CXX_COMPILER=/usr/bin/g++ -PCMAKE_C_COMPILER=/usr/bin/gcc` |
| Run Desktop | `./gradlew :composeApp:run -PCMAKE_CXX_COMPILER=/usr/bin/g++ -PCMAKE_C_COMPILER=/usr/bin/gcc` |
| Desktop Tests | `./gradlew :composeApp:desktopTest -PCMAKE_CXX_COMPILER=/usr/bin/g++ -PCMAKE_C_COMPILER=/usr/bin/gcc` |
| Android Debug | `./gradlew :composeApp:assembleFullDebug` |

The `-PCMAKE_CXX_COMPILER=/usr/bin/g++` flag is needed because the default clang++ can't link libstdc++ in this environment.

### Known Caveats

1. **Skiko GL context on VNC**: Skiko (Compose Desktop's rendering backend) cannot create an OpenGL context on TigerVNC displays. The app UI renders fine via software fallback, but the MPV video player surface requires OpenGL for texture-based rendering. On real desktop environments (physical display or GPU-backed virtual display), this works correctly.

2. **mediamp submodule**: The `mediamp` git submodule uses a detached HEAD. Changes committed inside it update the submodule reference in the parent repo. The native `libmediampv.so` is built automatically by Gradle's `configureCMakeDesktop` task.

3. **iOS targets disabled on Linux**: iOS cross-compilation is disabled (cinterop `commoncrypto` requires macOS). Build warnings about this are expected and harmless.

4. **Skiko version mismatch warning**: The mediamp submodule pulls Skiko 0.9.22.2 while Compose brings 0.144.5. This produces a warning but does not affect desktop builds.

5. **Addon URLs**: The `BAGUETTIO_ADDON_URL` and `CINEMATA_ADDON_URL` secrets contain Stremio-compatible addon manifest URLs needed for testing streaming playback.
