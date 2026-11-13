plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kiro.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kiro.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug key so the release APK installs on your own phone.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

// ---------------------------------------------------------------------------
// Roboto (Google Font). The variable TTF is downloaded once into res/font.
// If you are offline, download "Roboto[wdth,wght].ttf" from
// https://fonts.google.com/specimen/Roboto and save it as
// app/src/main/res/font/roboto.ttf
// ---------------------------------------------------------------------------
val robotoFile = layout.projectDirectory.file("src/main/res/font/roboto.ttf").asFile

val fetchRoboto by tasks.registering {
    onlyIf { !robotoFile.exists() }
    doLast {
        robotoFile.parentFile.mkdirs()
        val url = "https://raw.githubusercontent.com/google/fonts/main/ofl/roboto/Roboto%5Bwdth%2Cwght%5D.ttf"
        try {
            java.net.URI(url).toURL().openStream().use { input ->
                robotoFile.outputStream().use { out -> input.copyTo(out) }
            }
        } catch (e: Exception) {
            robotoFile.delete()
            throw GradleException(
                "Could not download Roboto (${e.message}). Download Roboto from Google Fonts and save it as " +
                    robotoFile.absolutePath
            )
        }
    }
}

tasks.named("preBuild") { dependsOn(fetchRoboto) }

// ---------------------------------------------------------------------------
// Optional: build the Rust search engine as part of the Gradle build.
//   ./gradlew assembleDebug -PbuildRust
// Needs: rustup, cargo-ndk, Android NDK (ANDROID_NDK_HOME). Without it the app
// still works - it uses an equivalent Kotlin ranker.
// ---------------------------------------------------------------------------
val buildRust by tasks.registering(Exec::class) {
    workingDir = file("../native")
    commandLine(
        "cargo", "ndk",
        "-t", "arm64-v8a", "-t", "armeabi-v7a", "-t", "x86_64",
        "-o", "../app/src/main/jniLibs",
        "build", "--release"
    )
}

if (project.hasProperty("buildRust")) {
    tasks.named("preBuild") { dependsOn(buildRust) }
}
