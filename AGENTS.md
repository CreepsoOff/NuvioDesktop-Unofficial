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

### Running the App

1. Set `DISPLAY=:1` and `LIBGL_ALWAYS_SOFTWARE=1` in the shell environment before running Gradle.
2. The Gradle `run` task passes DISPLAY/LIBGL through to the forked JVM process (configured in `composeApp/build.gradle.kts`).
3. On first launch, click "Continue Without Account" → "Add Profile" → enter the app.
4. Install addons via Profile > Settings > Content & Discovery > Addons (paste manifest URL + click "Install Addon").

### Known Caveats

1. **MPV player crash on VNC**: The native MPV library (`libmpv` 0.37.0) segfaults with `SIGSEGV` in `pthread_mutex_lock` when `mpv_create()` is invoked through the JNI bridge on a TigerVNC software-rendered display. The crash originates in the system libmpv, not our code. Video playback works on real desktop environments with proper GPU/display support. The app UI, addon system, search, and stream discovery all function correctly regardless.

2. **Skiko GL context on VNC**: Skiko cannot create an OpenGL context on TigerVNC displays; it falls back to software rendering. The app UI renders fine, but the GL-texture-based MPV surface path (`MpvMediampPlayerSurface`) requires a working GL context from Skiko.

3. **mediamp submodule**: The `mediamp` git submodule uses a detached HEAD. Changes committed inside it update the submodule reference in the parent repo. The native `libmediampv.so` is built automatically by Gradle's `configureCMakeDesktop` task.

4. **iOS targets disabled on Linux**: iOS cross-compilation is disabled (cinterop `commoncrypto` requires macOS). Build warnings about this are expected and harmless.

5. **Skiko version mismatch warning**: The mediamp submodule pulls Skiko 0.9.22.2 while Compose brings 0.144.5. This produces a warning but does not affect desktop builds.

6. **Addon URLs**: The `BAGUETTIO_ADDON_URL` and `CINEMATA_ADDON_URL` secrets contain Stremio-compatible addon manifest URLs needed for testing streaming playback.
