plugins {
    id("com.android.library")
    `maven-publish`
    signing
    kotlin("android")
    id("de.undercouch.download") version "5.5.0"
    alias(libs.plugins.detekt)
}

android {
    namespace="org.dbtools.android.room.sqliteorg"

    compileSdk = AndroidSdk.COMPILE

    defaultConfig {
        minSdk = AndroidSdk.MIN
        targetSdk = AndroidSdk.TARGET
        consumerProguardFiles("consumer-proguard-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
//        freeCompilerArgs = listOf("-module-name", Pom.LIBRARY_SQLITE_ORG_ARTIFACT_ID)
    }

    lint {
        abortOnError = true
        disable.addAll(listOf("InvalidPackage"))
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
    api(libs.androidx.room.runtime)
    implementation(libs.kermit)
    implementation(libs.okio)
    compileOnly(project(":sqlite-android"))

    // Test
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.mockK)
    testImplementation(libs.assertk)
    testImplementation(libs.androidx.room.testing)
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
        src("https://raw.githubusercontent.com/ICSEng/AndroidPublic/main/detekt/detektConfig-20231101.yml")
        dest("$projectDir/build/config/detektConfig.yml")
    }
}

// make sure when running detekt, the config file is downloaded
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    // Target version of the generated JVM bytecode. It is used for type resolution.
    this.jvmTarget = "17"
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

// ./gradlew clean assembleRelease publishReleasePublicationToMavenLocal
// ./gradlew clean assembleRelease publishReleasePublicationToMavenCentralRepository
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = Pom.GROUP_ID
            artifactId = Pom.LIBRARY_SQLITE_ORG_ARTIFACT_ID
            version = Pom.VERSION_NAME

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set(Pom.LIBRARY_SQLITE_ORG_NAME)
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

