const val KOTLIN_VERSION = "1.4.10"

object Deps {
    // Android (https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-master-dev/buildSrc/src/main/kotlin/androidx/build/dependencies/Dependencies.kt)
    const val ANDROID_DESUGAR_JDK_LIBS = "com.android.tools:desugar_jdk_libs:1.0.10"
    const val ANDROIDX_APPCOMPAT = "androidx.appcompat:appcompat:1.2.0"
    private const val LIFECYCLE_VERSION = "2.2.0"
    const val ARCH_LIFECYCLE_RUNTIME = "androidx.lifecycle:lifecycle-runtime-ktx:$LIFECYCLE_VERSION"
    const val ARCH_LIFECYCLE_LIVEDATA_KTX = "androidx.lifecycle:lifecycle-livedata-ktx:$LIFECYCLE_VERSION"

    // Code
    private const val COROUTINES_VERSION = "1.3.9"
    const val COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$COROUTINES_VERSION"
    const val TIMBER = "com.jakewharton.timber:timber:4.7.1"

    // Database
    private const val ROOM_VERSION = "2.2.5"
    const val ARCH_ROOM_RUNTIME = "androidx.room:room-runtime:$ROOM_VERSION"
    const val ARCH_ROOM_KTX = "androidx.room:room-ktx:$ROOM_VERSION"
    const val ARCH_ROOM_COMPILER = "androidx.room:room-compiler:$ROOM_VERSION"
    const val XERIAL_SQLITE = "org.xerial:sqlite-jdbc:3.32.3.2"

    // Test
    private const val JUNIT_VERSION = "5.7.0"
    const val TEST_JUNIT = "org.junit.jupiter:junit-jupiter:$JUNIT_VERSION"
    const val TEST_JUNIT_API = "org.junit.jupiter:junit-jupiter-api:$JUNIT_VERSION"
    const val TEST_JUNIT_ENGINE = "org.junit.jupiter:junit-jupiter-engine:$JUNIT_VERSION"

    const val TEST_MOCKITO_CORE = "org.mockito:mockito-core:3.5.10"
    const val TEST_ARCH_ROOM_TESTING = "androidx.room:room-testing:$ROOM_VERSION"
    const val TEST_MOCKITO_KOTLIN = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0"
}
