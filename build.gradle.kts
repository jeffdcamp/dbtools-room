import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    // ===== Detekt =====
    // Known KMP issues https://github.com/detekt/detekt/issues/5611
    apply(plugin = rootProject.libs.plugins.detekt.get().pluginId).also {
        // download detekt config file
        tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadDetektConfig") {
            download {
                onlyIf { !file("$projectDir/build/config/detektConfig.yml").exists() }
                src("https://mobile-cdn.churchofjesuschrist.org/android/build/detekt/detektConfig-20231101.yml")
                dest("$projectDir/build/config/detektConfig.yml")
            }
        }

        // make sure when running detekt, the config file is downloaded
        tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
            // Target version of the generated JVM bytecode. It is used for type resolution.
            this.jvmTarget = JvmTarget.JVM_17.target
            dependsOn("downloadDetektConfig")
        }

        detekt {
            allRules = true // fail build on any finding
            buildUponDefaultConfig = true // preconfigure defaults
            config.setFrom(files("$projectDir/build/config/detektConfig.yml")) // point to your custom config defining rules to run, overwriting default behavior
        }

        tasks.withType<io.gitlab.arturbosch.detekt.Detekt> {
            setSource(files(project.projectDir))
            exclude("**/build/**")
            exclude("**/*.kts")
            exclude("**/Platform.*.kt")


            exclude {
                it.file.relativeTo(projectDir).startsWith(buildDir.relativeTo(projectDir))
            }
        }

        tasks.register("detektAll") {
            dependsOn(tasks.withType<io.gitlab.arturbosch.detekt.Detekt>())
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
