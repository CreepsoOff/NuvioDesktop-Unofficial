-dontshrink
-dontoptimize
-dontobfuscate
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keep class com.nuvio.app.** { *; }
-keep interface com.nuvio.app.** { *; }
-keep enum com.nuvio.app.** { *; }

-keep class coil3.** { *; }
-keep interface coil3.** { *; }
-keep enum coil3.** { *; }

-keep class io.ktor.** { *; }
-keep interface io.ktor.** { *; }
-keep enum io.ktor.** { *; }

-keep class kotlinx.serialization.** { *; }
-keep interface kotlinx.serialization.** { *; }
-keep enum kotlinx.serialization.** { *; }

-keep class dev.whyoleg.** { *; }
-keep interface dev.whyoleg.** { *; }
-keep enum dev.whyoleg.** { *; }

-keep class dev.chrisbanes.haze.** { *; }
-keep interface dev.chrisbanes.haze.** { *; }
-keep enum dev.chrisbanes.haze.** { *; }

-keep class com.typesafe.config.** { *; }
-keep interface com.typesafe.config.** { *; }
-keep enum com.typesafe.config.** { *; }

-keep class io.ktor.client.engine.java.** { *; }
-keep class io.ktor.serialization.kotlinx.json.** { *; }
-keep class coil3.network.ktor3.internal.** { *; }
-keep class dev.whyoleg.cryptography.providers.jdk.** { *; }
-keep class io.ktor.server.config.** { *; }

-dontwarn androidx.compose.ui.test.**
-dontwarn dev.chrisbanes.haze.**
-dontwarn com.google.common.truth.**
-dontwarn org.objectweb.asm.**

# OkHttp ships optional integrations for Android, Conscrypt, BouncyCastle and
# OpenJSSE that are runtime-probed via reflection. None of them are present on
# the Desktop JVM classpath, so ProGuard sees missing references and fails the
# release build. Silence those references for the Windows Desktop distribution.
-dontwarn android.**
-dontwarn dalvik.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.internal.platform.android.**
-dontwarn okhttp3.internal.platform.AndroidPlatform
-dontwarn okhttp3.internal.platform.Android10Platform
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn okhttp3.internal.platform.ConscryptPlatform$**
-dontwarn okhttp3.internal.platform.BouncyCastlePlatform
-dontwarn okhttp3.internal.platform.BouncyCastlePlatform$**
-dontwarn okhttp3.internal.platform.OpenJSSEPlatform
-dontwarn okhttp3.internal.platform.OpenJSSEPlatform$**
