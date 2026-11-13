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

    // Fonts are generated at build time into build/generated/fonts (see prepareFonts below).
    sourceSets.getByName("main").res.srcDir(layout.buildDirectory.dir("generated/fonts/res"))
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

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

// ---------------------------------------------------------------------------
// Fonts. The app reads four font slots: R.font.app_light / app_regular / app_medium / app_bold.
//   Default: Roboto, from fonts/roboto (committed, open-licensed).
//   Your own fonts: put light / regular / medium / bold (.ttf or .otf, any case) in fonts-private/
//   at the project root. That folder is git-ignored, so licensed or proprietary fonts never end up in
//   the repository or in GitHub Actions builds - those always use Roboto.
// ---------------------------------------------------------------------------
val prepareFonts by tasks.registering {
    val robotoDir = rootProject.file("fonts/roboto")
    val privateDir = rootProject.file("fonts-private")
    val outDir = layout.buildDirectory.dir("generated/fonts/res/font")
    outputs.upToDateWhen { false }
    doLast {
        val dest = outDir.get().asFile
        dest.deleteRecursively()
        dest.mkdirs()
        val used = mutableListOf<String>()
        for (slot in listOf("light", "regular", "medium", "bold")) {
            val custom = privateDir.listFiles { f ->
                f.isFile && f.nameWithoutExtension.equals(slot, ignoreCase = true) &&
                    f.extension.lowercase() in listOf("ttf", "otf")
            }?.firstOrNull()
            val src = custom ?: File(robotoDir, "roboto_$slot.ttf")
            src.copyTo(File(dest, "app_$slot.${src.extension.lowercase()}"), overwrite = true)
            used += "$slot=${if (custom != null) "fonts-private/" + custom.name else "Roboto"}"
        }
        logger.lifecycle("Kiro fonts: " + used.joinToString(", "))
    }
}

tasks.named("preBuild") { dependsOn(prepareFonts) }
