plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "app.honguyen.forge.designsystem"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
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
    implementation(project(":lib-compose-utils"))

    // Compose — api, so consumers of the theme inherit its UI surface.
    api(platform(libs.androidx.compose.bom))
    api(libs.bundles.compose.ui)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
