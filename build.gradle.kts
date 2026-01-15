import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.vanniktechPublishing) apply false

    alias(libs.plugins.detekt)
    alias(libs.plugins.download)
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

allprojects {
    apply(plugin = rootProject.libs.plugins.detekt.get().pluginId)
    apply(plugin = rootProject.libs.plugins.download.get().pluginId)

    // ===== Detekt =====
    // download detekt config file
    tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadDetektConfig") {
        download {
            onlyIf { !file("$projectDir/build/config/detektConfig.yml").exists() }
            src("https://mobile-cdn.churchofjesuschrist.org/android/build/detekt/v2/detektConfig-latest.yml")
            dest("$projectDir/build/config/detektConfig.yml")
        }
    }

    // ./gradlew detekt
    // ./gradlew detektDebug (support type checking)
    detekt {
        allRules = true // fail build on any finding
        buildUponDefaultConfig = true // preconfigure defaults
        config.setFrom("$projectDir/build/config/detektConfig.yml") // point to your custom config defining rules to run, overwriting default behavior
        baseline = file("$projectDir/config/detektBaseline.xml") // a way of suppressing issues before introducing detekt (./gradlew detektBaseline)
        source.setFrom("src/main/java", "src/main/kotlin", "src/commonMain/kotlin", "src/androidMain/kotlin", "src/jvmMain/kotlin", "src/iosMain/kotlin")
    }

    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        dependsOn("downloadDetektConfig")

        // ignore ImageVector files
        exclude("**/ui/compose/icons/**")

        reports {
            html.required.set(true) // observe findings in your browser with structure and code snippets
//            xml.required.set(true) // checkstyle like format mainly for integrations like Jenkins
//            txt.required.set(true) // similar to the console output, contains issue signature to manually edit baseline files
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
