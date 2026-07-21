plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "app.honguyen.forge.datastore"
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

    // protobuf-gradle-plugin registers its output through AGP's variant API. Gradle honors
    // that, but Android Studio does not surface it as a source root, so the generated proto
    // package reads as unresolved in the editor while compiling perfectly (IDEA-209418).
    // Naming the directories here is purely for the IDE: AGP already carries the real task
    // dependency and de-duplicates the sources rather than compiling them twice.
    //
    // These must be plain Strings. AGP 9 rejects Provider instances on this API because it
    // cannot tell generated from static directories, and the workaround flag it suggests
    // (android.sourceset.disallowProvider) is a project-wide opt-out not worth taking.
    sourceSets {
        getByName("debug") {
            java.srcDir("build/generated/java/generateDebugProto/java")
        }
        getByName("release") {
            java.srcDir("build/generated/java/generateReleaseProto/java")
        }
    }
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                // The lite runtime drops reflection and descriptors, which is what makes
                // protobuf viable on Android: far smaller method count and APK footprint.
                register("java") {
                    option("lite")
                }
            }
        }
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
    api(libs.androidx.datastore)
    api(libs.protobuf.javalite)

    implementation(project(":lib-coroutines"))

    implementation(libs.hilt.android)
    implementation(libs.timber)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}
