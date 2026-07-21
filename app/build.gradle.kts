plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "app.honguyen.forge"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "app.honguyen.forge"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Hilt needs its own Application subclass in instrumented tests; ForgeTestRunner
        // swaps it in for ForgeApp.
        testInstrumentationRunner = "app.honguyen.forge.ForgeTestRunner"

        // The engine is built in :feature-camera-preview, but ABI filtering is a packaging
        // concern: without this the APK also picks up armeabi-v7a/x86 variants of native
        // libs pulled in by dependencies.
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    // The model asset itself lives in :feature-camera-preview, but APK packaging is the
    // app module's job — assets merged in from a library are still compressed according
    // to this setting. Keeping the .tflite uncompressed lets it be memory-mapped / read
    // directly from the APK rather than inflated into memory.
    androidResources {
        noCompress.add("tflite")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/config/detekt/detekt.yml")
}

ktlint {
    android.set(true)
}

dependencies {
    // Design system (theme + shared UI foundations)
    implementation(project(":lib-design-system"))
    implementation(project(":lib-compose-utils"))

    // Features
    implementation(project(":feature-camera-preview"))

    // Data
    implementation(project(":lib-data"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Splashscreen
    implementation(libs.androidx.core.splashscreen)

    // Database - Room
    implementation(libs.room.ktx)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // DI - Hilt
    implementation(libs.bundles.hilt)
    ksp(libs.hilt.compiler)

    // Navigation
    implementation(libs.androidx.compose.navigation)
    implementation(libs.kotlinx.serialization.json)

    // Allow use of java.time.Instant below API 26
    coreLibraryDesugaring(libs.desugarJdkLibs)

    // Logging
    implementation(libs.timber)

    testImplementation(libs.junit)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
