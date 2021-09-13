import com.android.build.gradle.BaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lint {
        isAbortOnError = true
        disable("InvalidPackage")
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

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-module-name", Pom.LIBRARY_JDBC_ARTIFACT_ID)
    }
}

dependencies {
    api(libs.androidx.lifecycle.runtime)
    api(libs.androidx.room.runtime)
    compileOnly(libs.xerial.sqlite)

    // Test
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.engine)
    testImplementation(libs.mockK)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.room.ktx)
    testImplementation(libs.androidx.lifecycle.livedata)
    kaptTest(libs.androidx.room.compiler)
    testImplementation(libs.xerial.sqlite)
}

// ===== TEST TASKS =====

// create JUnit reports
tasks.withType<Test> {
    useJUnitPlatform()
}

// ===== Maven Deploy =====

// ./gradlew clean assembleRelease publishMavenPublicationToMavenLocal
// ./gradlew clean assembleRelease publishMavenPublicationToMavenCentralRepository

tasks.register<Jar>("sourcesJar") {
    //    from(android.sourceSets.getByName("main").java.sourceFiles)
    from(project.the<BaseExtension>().sourceSets["main"].java.srcDirs)
    archiveClassifier.set("sources")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = Pom.GROUP_ID
            artifactId = Pom.LIBRARY_JDBC_ARTIFACT_ID
            version = Pom.VERSION_NAME
            artifact(tasks["sourcesJar"])
            afterEvaluate { artifact(tasks.getByName("bundleReleaseAar")) }
            pom {
                name.set(Pom.LIBRARY_JDBC_NAME)
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

            // add dependencies to pom.xml
            pom.withXml {
                val dependenciesNode = asNode().appendNode("dependencies")
                configurations.implementation.get().allDependencies.forEach {
                    val dependencyNode = dependenciesNode.appendNode("dependency")
                    dependencyNode.appendNode("groupId", it.group)
                    dependencyNode.appendNode("artifactId", it.name)
                    dependencyNode.appendNode("version", it.version)
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
    sign(publishing.publications["maven"])
}