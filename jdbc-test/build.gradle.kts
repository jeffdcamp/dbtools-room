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

        buildConfigField("String", "SCHEMA_PATH", "\"schemas\"")

        // used by Room, to test migrations
        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-module-name", Pom.LIBRARY_JDBC_TEST_ARTIFACT_ID)
    }

    lint {
        abortOnError = true
        disable.addAll(listOf("InvalidPackage"))
    }
}

dependencies {
    implementation(project(":jdbc")) // NOTE: for pom.xml publishing, this type of dependency needs to be set manually in the <publishing> section (below)

    // Android
    implementation(libs.androidx.lifecycle.livedata)

    // Code
    implementation(libs.kotlin.coroutines.android)
    implementation(libs.timber)

    // Test
    implementation(libs.junit.jupiter)
    implementation(libs.junit.engine)
    implementation(libs.mockK)
    testImplementation(libs.truth)
    implementation(libs.androidx.room.testing)

    // Test (internal only)
    kaptTest(libs.androidx.room.compiler)
    testImplementation(libs.xerial.sqlite)
}

// ===== TEST TASKS =====

// create JUnit reports
tasks.withType<Test> {
    useJUnitPlatform()
}

// ===== Maven Deploy =====

// ./gradlew clean assembleRelease publishReleasePublicationToMavenLocal
// ./gradlew clean assembleRelease publishReleasePublicationToMavenCentralRepository
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = Pom.GROUP_ID
            artifactId = Pom.LIBRARY_JDBC_TEST_ARTIFACT_ID
            version = Pom.VERSION_NAME

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set(Pom.LIBRARY_JDBC_TEST_NAME)
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
}

signing {
    sign(publishing.publications["release"])
}