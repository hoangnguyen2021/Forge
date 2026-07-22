plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "app.honguyen.forge.camera.preview"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                // Build only our engine (and its deps); skip the LiteRT SDK test executables.
                targets += "forge_engine"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // Package the vendored LiteRT runtime (libLiteRt.so, per ABI) into the AAR so the
    // dynamic linker can find it at load time.
    sourceSets {
        getByName("main") {
            jniLibs.directories.add("src/main/cpp/third_party/litert/jni")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
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
    implementation(project(":lib-compose-utils"))
    implementation(project(":lib-design-system"))

    // AndroidX foundations
    implementation(libs.androidx.core.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.compose.ui)

    // Logging
    implementation(libs.timber)

    // Unit tests
    testImplementation(libs.junit)

    // Instrumented tests
    androidTestImplementation(libs.bundles.androidx.instrumented.test)

    // Debug tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
}
