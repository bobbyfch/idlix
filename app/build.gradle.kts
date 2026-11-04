import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(FileInputStream(file))
    }
}

android {
    namespace = "com.idlix.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.idlix.tv"
        minSdk = 26
        targetSdk = 35
        // CI run numbers are monotonic for this workflow, so every published
        // APK can update the previous one without a version-code downgrade.
        // Keep local release builds above the previous public version code.
        versionCode = System.getenv("IDLIX_VERSION_CODE")?.toIntOrNull() ?: 1_200_000
        // Release version is exported once by Actions; same value used in APK,
        // GitHub tag, release title, and download filename.
        versionName = System.getenv("IDLIX_RELEASE_VERSION")
            ?.takeIf { it.matches(Regex("""\d+\.\d+\.\d+""")) } ?: "1.0.0"

        // Instrumentation launch test exercises the real Compose draw pipeline.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val backendUrl = localProperties.getProperty("backendApiUrl") ?: "https://z2.idlixku.com/"
        buildConfigField("String", "BACKEND_API_URL", "\"$backendUrl\"")

        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            debugSymbolLevel = "FULL"
        }
    }

    signingConfigs {
        create("release") {
            val storeFilePath = localProperties.getProperty("storeFile") ?: "release-key.jks"
            storeFile = file(storeFilePath)
            storePassword = localProperties.getProperty("storePassword")
            keyAlias = localProperties.getProperty("keyAlias")
            keyPassword = localProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.addAll(
            "-opt-in=androidx.tv.material3.ExperimentalTvMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.jsoup)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM & Core
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    // Explicit opaque HazeStyle background prevents the confirmed 1.1.6 crash.
    // Only phone floating controls use live blur; TV retains the safe material.
    implementation(libs.haze)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Android TV Compose (Netflix-style UI)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Media3 / ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)

    // Network & Serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Coil for TV image loading
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")

}
