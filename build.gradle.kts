import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.vanniktechPublishing) apply false

    alias(libs.plugins.versions)
}

// ===== Gradle Dependency Check =====
// ./gradlew dependencyUpdates -Drevision=release
// ./gradlew dependencyUpdates -Drevision=release --refresh-dependencies
tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(
            version = candidate.version,
            includeStablePreRelease = false
        )
    }
}

fun isNonStable(version: String, includeStablePreRelease: Boolean): Boolean {
    val stablePreReleaseKeyword = listOf("RC", "BETA").any { version.uppercase().contains(it) }
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+$".toRegex()
    val isStable = if (includeStablePreRelease) {
        stableKeyword || regex.matches(version) || stablePreReleaseKeyword
    } else {
        stableKeyword || regex.matches(version)
    }
    return isStable.not()
}
