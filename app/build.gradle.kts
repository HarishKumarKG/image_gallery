plugins {
    alias(libs.plugins.harish.android.application)
    alias(libs.plugins.harish.hilt)
}

android {
    namespace = "com.harish.gallery"

    defaultConfig {
        applicationId = "com.harish.gallery"
        // CI (GitHub Actions) passes these from the tag / run number; local builds use the defaults
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME") ?: "1.0"
    }

    // Release signing comes from environment variables (GitHub Actions secrets).
    // Nothing secret is stored in the repo; without them, release builds are simply unsigned.
    val releaseKeystore = System.getenv("KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    lint {
        // Media3 marks some APIs as "unstable"; fine to use in an app
        disable += "UnsafeOptInUsageError"
    }
}

dependencies {
    // Every feature (screens) + the core they share. Features never depend on each other.
    implementation(project(":feature:photos"))
    implementation(project(":feature:albums"))
    implementation(project(":feature:viewer"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:search"))
    implementation(project(":feature:locked"))
    implementation(project(":feature:settings"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
