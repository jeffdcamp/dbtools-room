@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
//    alias(libs.plugins.kover) // Does not seem to work with "com.android.kotlin.multiplatform.library"
    alias(libs.plugins.download)
    alias(libs.plugins.vanniktechPublishing)
    signing
}

kotlin {
    applyDefaultHierarchyTemplate()

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }

    androidLibrary {
        namespace = "com.dbtools.room"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilations.configureEach {
            compilerOptions.configure {
                jvmTarget.set(
                    JvmTarget.JVM_17
                )
            }
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    linuxX64()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        macosX64(),
        macosArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "dbtools-room"
            binaryOption("bundleId", "org.dbtools.room")
            binaryOption("bundleVersion", property("version") as? String ?: "0.0.0")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                //put your multiplatform dependencies here
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.kotlin.datetime)
                implementation(libs.room.runtime)
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

// ./gradlew koverHtmlReport
// ./gradlew koverVerify
//kover {
//    reports {
//        verify {
//            rule {
//                minBound(0)
//            }
//        }
//    }
//}

// ./gradlew clean build check publishToMavenLocal
// ./gradlew clean build check publishToMavenCentral
mavenPublishing {
    coordinates("org.dbtools", "dbtools-room", "9.1.0") // set version in gradle.properties also
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("DBTools Room")
        description.set("DBTools Room")
        url.set("https://github.com/jeffdcamp/dbtools-room")
        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("jcampbell")
                name.set("Jeff Campbell")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/jeffdcamp/dbtools-room.git")
            developerConnection.set("scm:git:git@github.com:jeffdcamp/dbtools-room.git")
            url.set("https://github.com/jeffdcamp/dbtools-room")
        }
    }
}

signing {
    setRequired {
        findProperty("signing.keyId") != null
    }

    publishing.publications.all {
        sign(this)
    }
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
        named("compileTestKotlinIosX64") {
            dependsOn(named("signIosX64Publication"))
        }
        named("compileTestKotlinMacosArm64") {
            dependsOn(named("signMacosArm64Publication"))
        }
        named("compileTestKotlinMacosX64") {
            dependsOn(named("signMacosX64Publication"))
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
