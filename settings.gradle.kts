rootProject.name = "Nuvio"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")

val mediampRoot = settingsDir.resolve("mediamp").takeIf { candidate ->
    candidate.resolve("settings.gradle.kts").isFile &&
        candidate.resolve("mediamp-api/build.gradle.kts").isFile &&
        candidate.resolve("mediamp-mpv/build.gradle.kts").isFile &&
        candidate.resolve("mediamp-internal-utils/build.gradle.kts").isFile
}

if (mediampRoot != null) {
    includeBuild(mediampRoot) {
        dependencySubstitution {
            substitute(module("org.openani.mediamp:mediamp-api")).using(project(":mediamp-api"))
            substitute(module("org.openani.mediamp:mediamp-mpv")).using(project(":mediamp-mpv"))
            substitute(module("org.openani.mediamp:mediamp-internal-utils")).using(project(":mediamp-internal-utils"))
        }
    }
}
