plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

android {
    compileSdkVersion(AndroidSdk.COMPILE)

    defaultConfig {
        minSdkVersion(AndroidSdk.MIN)
        targetSdkVersion(AndroidSdk.TARGET)

        versionCode = 1000
        versionName = "1.0.0"

        multiDexEnabled = true

        // used by Room, to test migrations
        javaCompileOptions {
            annotationProcessorOptions {
                arguments = mapOf("room.schemaLocation" to "$projectDir/schema")
            }
        }

        // Espresso
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
    }

    lintOptions {
        isAbortOnError = true
        disable("InvalidPackage")
    }

    buildTypes {
        getByName("debug") {
            versionNameSuffix = " DEV"
            applicationIdSuffix = ".dev"
        }
        getByName("release") {
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
    coreLibraryDesugaring(Deps.ANDROID_DESUGAR_JDK_LIBS)
    implementation(Deps.ANDROIDX_APPCOMPAT)

    // Code
    implementation(Deps.KOTLIN_STD_LIB)
    implementation(Deps.COROUTINES)
    implementation(Deps.TIMBER)

    // UI

    // === Android Architecture Components ===
    implementation(Deps.ARCH_LIFECYCLE_EXT)
    implementation(Deps.ARCH_LIFECYCLE_RUNTIME)

    // Database
    implementation(Deps.ARCH_ROOM_RUNTIME)
    kapt(Deps.ARCH_ROOM_COMPILER)

    // Test
    testImplementation(Deps.XERIAL_SQLITE)
}

// ===== TEST TASKS =====

// create JUnit reports
tasks.withType<Test> {
    useJUnitPlatform()
}
