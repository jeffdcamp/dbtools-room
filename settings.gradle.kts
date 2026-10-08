pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

// Fixed name (instead of the checkout directory's) so the klib ABI dump in dbtools-room/api/ matches in any checkout
rootProject.name = "dbtools-room"

include(":dbtools-room")
