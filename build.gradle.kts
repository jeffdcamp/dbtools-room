import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath(libs.android.gradlePluginClasspath)
        classpath(libs.kotlin.gradlePluginClasspath)
        classpath(libs.gradleVersions.gradlePluginClasspath)
    }
}

plugins {
    id("com.autonomousapps.dependency-analysis") version "1.19.0"
}

allprojects {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
//        maven { url "https://oss.sonatype.org/content/repositories/snapshots" }
    }

    // Gradle Dependency Check
    apply(plugin = "com.github.ben-manes.versions") // ./gradlew dependencyUpdates -Drevision=release
    val excludeVersionContaining = listOf("alpha", "eap") // example: "alpha", "beta"
    val ignoreArtifacts = listOf("room-compiler", "room-runtime", "room-testing", "room-ktx") // some artifacts may be OK to check for "alpha"... add these exceptions here

    tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
        resolutionStrategy {
            componentSelection {
                all {
                    if (ignoreArtifacts.contains(candidate.module).not()) {
                        val rejected = excludeVersionContaining.any { qualifier ->
                            candidate.version.matches(Regex("(?i).*[.-]$qualifier[.\\d-+]*"))
                        }
                        if (rejected) {
                            reject("Release candidate")
                        }
                    }
                }
            }
        }
    }
}

// ===== Dependency Analysis =====
// ./gradlew projectHealth
dependencyAnalysis {
    issues {
        all {
            onAny {
                ignoreKtx(true)
                severity("fail")
            }
            onUnusedDependencies {
                exclude(
                    ":jdbc",
                    depGroupAndName(libs.xerial.sqlite), // loaded by reflection in UnitTest
//                    depGroupAndName(libs.compose.ui.tooling), // Compose Previews
                )
            }
            onUsedTransitiveDependencies { severity("ignore") }
            onIncorrectConfiguration { severity("ignore") }
            onCompileOnly { severity("ignore") }
            onRuntimeOnly { severity("ignore") }
            onUnusedAnnotationProcessors {
                exclude(
//                    depGroupAndName(libs.google.hilt.compiler)
                )
            }
        }
    }
}

fun depGroupAndName(dependency: Provider<MinimalExternalModuleDependency>): String {
    return dependency.get().let { "${it.group}:${it.name}" }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
