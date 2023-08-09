plugins {
    id("com.android.application")
    kotlin("android")
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.dbtools.sample.roomsqlite"

    compileSdk = AndroidSdk.COMPILE

    defaultConfig {
        minSdk = AndroidSdk.MIN
        targetSdk = AndroidSdk.TARGET

        versionCode = 1000
        versionName = "1.0.0"

        // used by Room, to test migrations
        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.schemaLocation", "$projectDir/schema")
            }
        }

        // Espresso
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        viewBinding = true
    }

    lint {
        abortOnError = true
        disable.addAll(listOf("InvalidPackage"))
    }

    buildTypes {
        debug {
            versionNameSuffix = " DEV"
            applicationIdSuffix = ".dev"
        }
        release {
            versionNameSuffix = ""
        }
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
}

dependencies {
    implementation(project(":library"))
    implementation(project(":sqliteorg"))
    implementation(project(":jdbc"))
    implementation(project(":sqlite-android"))

    // Android
    coreLibraryDesugaring(libs.android.desugar)
    implementation(libs.androidx.appcompat)

    // Code
    implementation(libs.kotlin.coroutines.android)
    implementation(libs.timber)

    // UI

    // === Android Architecture Components ===
    implementation(libs.androidx.lifecycle.runtime)

    // Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Test
    testImplementation(libs.xerial.sqlite)
}

// ===== TEST TASKS =====

// create JUnit reports
tasks.withType<Test> {
    useJUnitPlatform()
}
