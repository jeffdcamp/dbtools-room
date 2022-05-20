plugins {
    id("com.android.library")
    `maven-publish`
    signing
    kotlin("android")
    kotlin("kapt")
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