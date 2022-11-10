plugins {
    id("com.android.library")
    `maven-publish`
    signing
    kotlin("android")
    kotlin("kapt")
    id("de.undercouch.download") version "5.3.0"
    alias(libs.plugins.detekt)
}

android {
    compileSdk = AndroidSdk.COMPILE

    defaultConfig {
        minSdk = AndroidSdk.MIN
        targetSdk = AndroidSdk.TARGET
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-module-name", Pom.LIBRARY_ARTIFACT_ID)
    }

    lint {
        abortOnError = true
        disable.addAll(listOf(
            "InvalidPackage",
            "NullSafeMutableLiveData" // this rule is crashing lint check for RoomLiveData file
        ))
    }

    sourceSets {
        getByName("main") {
            java.srcDir("src/main/kotlin")
        }
        getByName("test") {
            java.srcDir("src/test/kotlin")
        }
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    api(libs.androidx.lifecycle.runtime)
    api(libs.androidx.lifecycle.livedata)
    api(libs.androidx.room.runtime)
    api(libs.kotlin.coroutines.android)
    api(libs.timber)

    // Test
    testImplementation(project(":jdbc"))
    testImplementation(libs.xerial.sqlite)
    testImplementation(libs.androidx.room.ktx)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.mockK)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.room.testing)
    kaptTest(libs.androidx.room.compiler)
}

// ===== TEST TASKS =====

// create JUnit reports
tasks.withType<Test> {
    useJUnitPlatform()
}

// ===== Detekt =====

// download detekt config file
tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadDetektConfig") {
    download {
        onlyIf { !file("$projectDir/build/config/detektConfig.yml").exists() }
        src("https://raw.githubusercontent.com/ICSEng/AndroidPublic/main/detekt/detektConfig-20221107.yml")
        dest("$projectDir/build/config/detektConfig.yml")
    }
}

// make sure when running detekt, the config file is downloaded
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    // Target version of the generated JVM bytecode. It is used for type resolution.
    this.jvmTarget = "1.8"
    dependsOn("downloadDetektConfig")
}

// ./gradlew detekt
// ./gradlew detektDebug (support type checking)
detekt {
    allRules = true // fail build on any finding
    buildUponDefaultConfig = true // preconfigure defaults
    config = files("$projectDir/build/config/detektConfig.yml") // point to your custom config defining rules to run, overwriting default behavior
//    baseline = file("$projectDir/config/baseline.xml") // a way of suppressing issues before introducing detekt
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    // ignore ImageVector files
    exclude("**/ui/compose/icons/**")

    reports {
        html.required.set(true) // observe findings in your browser with structure and code snippets
        xml.required.set(true) // checkstyle like format mainly for integrations like Jenkins
        txt.required.set(true) // similar to the console output, contains issue signature to manually edit baseline files
    }
}

// ===== Maven Deploy =====

// ./gradlew clean check assembleRelease publishReleasePublicationToMavenLocal
// ./gradlew clean check assembleRelease publishReleasePublicationToSoupbowlRepository
// ./gradlew clean check assembleRelease publishReleasePublicationToMavenCentralRepository
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = Pom.GROUP_ID
            artifactId = Pom.LIBRARY_ARTIFACT_ID
            version = Pom.VERSION_NAME

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set(Pom.LIBRARY_NAME)
                description.set(Pom.POM_DESCRIPTION)
                url.set(Pom.URL)
                licenses {
                    license {
                        name.set(Pom.LICENCE_NAME)
                        url.set(Pom.LICENCE_URL)
                        distribution.set(Pom.LICENCE_DIST)
                    }
                }
                developers {
                    developer {
                        id.set(Pom.DEVELOPER_ID)
                        name.set(Pom.DEVELOPER_NAME)
                    }
                }
                scm {
                    url.set(Pom.SCM_URL)
                    connection.set(Pom.SCM_CONNECTION)
                    developerConnection.set(Pom.SCM_DEV_CONNECTION)
                }
            }
        }
    }
    repositories {
        maven {
            name = "MavenCentral"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            credentials {
                val sonatypeNexusUsername: String? by project
                val sonatypeNexusPassword: String? by project
                username = sonatypeNexusUsername ?: ""
                password = sonatypeNexusPassword ?: ""
            }
        }
    }
    repositories {
        maven {
            name = "Soupbowl"
            url = uri("http://192.168.0.5:8082/nexus/content/repositories/releases/")
            credentials {
                val soupbowlNexusUsername: String? by project
                val soupbowlNexusPassword: String? by project
                username = soupbowlNexusUsername ?: ""
                password = soupbowlNexusPassword ?: ""
            }
            isAllowInsecureProtocol = true
        }
    }
}

signing {
    sign(publishing.publications["release"])
}