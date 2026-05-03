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

-keep class com.typesafe.config.** { *; }
-keep interface com.typesafe.config.** { *; }
-keep enum com.typesafe.config.** { *; }

-keep class io.ktor.client.engine.java.** { *; }
-keep class io.ktor.serialization.kotlinx.json.** { *; }
-keep class coil3.network.ktor3.internal.** { *; }
-keep class dev.whyoleg.cryptography.providers.jdk.** { *; }
-keep class io.ktor.server.config.** { *; }

-dontwarn androidx.compose.ui.test.**
-dontwarn com.google.common.truth.**
-dontwarn org.objectweb.asm.**