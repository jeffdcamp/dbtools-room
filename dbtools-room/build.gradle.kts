@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kover)
    alias(libs.plugins.download)
    alias(libs.plugins.detekt)
    alias(libs.plugins.vanniktechPublishing)
}

kotlin {
    applyDefaultHierarchyTemplate()

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }

    android {
        namespace = "com.dbtools.room"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Enable this if there are any Android resource files
        // androidResources.enable = true

        // Host-side (JVM) unit tests for androidMain code (no device required).
        withHostTestBuilder {
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    linuxX64()

    // Mac / iOS
    val appleTargets = listOf(
        iosArm64(),
        iosSimulatorArm64(),
        macosArm64(),
    )
    // Klibs cross-compile on any host (used for maven publishing), but linking an Apple framework
    // binary requires a macOS host. Only declare the frameworks on Mac so Linux CI can still
    // assemble/publish the klibs without failing on the framework link tasks.
    if (HostManager.hostIsMac) {
        appleTargets.forEach {
            it.binaries.framework {
                baseName = "dbtools-room"
                binaryOption("bundleId", "org.dbtools.room")
                val version: String by project
                binaryOption("bundleVersion", version)
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                //put your multiplatform dependencies here
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.kotlin.datetime)
                implementation(libs.kotlin.io.core)
                implementation(libs.room.runtime)
                implementation(libs.kotlin.atomicfu)
                implementation(libs.okio)
                implementation(libs.kermit)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.coroutines.test)
                implementation(libs.assertk)
                implementation(libs.kotlin.coroutines.test)
            }
        }
    }
}

// ===== Detekt =====
// download detekt config file
tasks.register<Download>("downloadDetektConfig") {
    download {
        onlyIf { !file("$projectDir/build/config/detektConfig.yml").exists() }
        src("https://mobile-cdn.churchofjesuschrist.org/android/build/detekt/v2/detektConfig-latest.yml")
        dest("$projectDir/build/config/detektConfig.yml")
    }
}

// ./gradlew detekt
detekt {
    source.setFrom("src/commonMain/kotlin", "src/androidMain/kotlin", "src/jvmMain/kotlin", "src/linuxMain/kotlin", "src/appleMain/kotlin")
    allRules = true // fail build on any finding
    buildUponDefaultConfig = true // preconfigure defaults
    config.setFrom(files("$projectDir/build/config/detektConfig.yml")) // point to your custom config defining rules to run, overwriting default behavior
    // baseline = file("$projectDir/config/detektBaseline.xml") // a way of suppressing issues before introducing detekt
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    dependsOn("downloadDetektConfig")

    // ignore ImageVector files
    exclude("**/ui/compose/icons/**")

    reports {
        html.required.set(true) // observe findings in your browser with structure and code snippets
    }
}

// ./gradlew koverHtmlReport
// ./gradlew koverVerify
kover {
    reports {
        verify {
            rule {
                minBound(0)
            }
        }
    }
}

// ./gradlew clean build check publishToMavenLocal
// ./gradlew clean build check publishAllPublicationsToMavenCentralRepository
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    configure(
        com.vanniktech.maven.publish.KotlinMultiplatform(
            javadocJar = com.vanniktech.maven.publish.JavadocJar.Empty(),
            sourcesJar = true,
            androidVariantsToPublish = listOf("release"),
        )
    )
}

// TODO: remove after following issues are fixed
// https://github.com/gradle/gradle/issues/26091
// https://youtrack.jetbrains.com/issue/KT-46466
tasks {
    withType<PublishToMavenLocal> {
        dependsOn(withType<Sign>())
    }

    withType<PublishToMavenRepository> {
        dependsOn(withType<Sign>())
    }

    if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
        named("compileTestKotlinIosArm64") {
            dependsOn(named("signIosArm64Publication"))
        }
        named("compileTestKotlinIosSimulatorArm64") {
            dependsOn(named("signIosSimulatorArm64Publication"))
        }
        named("compileTestKotlinMacosArm64") {
            dependsOn(named("signMacosArm64Publication"))
        }
        // Mac can also do Linux signing
        named("compileTestKotlinLinuxX64") {
            dependsOn(named("signLinuxX64Publication"))
        }
    }

    if (org.gradle.internal.os.OperatingSystem.current().isLinux) {
        named("compileTestKotlinLinuxX64") {
            dependsOn(named("signLinuxX64Publication"))
        }
    }
}
