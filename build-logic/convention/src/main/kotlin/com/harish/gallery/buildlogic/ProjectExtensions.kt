package com.harish.gallery.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** The version catalog (gradle/libs.versions.toml), usable from plugin code. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** One library from the catalog, e.g. lib("androidx-core-ktx"). */
internal fun Project.lib(alias: String) = libs.findLibrary(alias).get()

internal const val COMPILE_SDK = 37
internal const val TARGET_SDK = 37
internal const val MIN_SDK = 30

/** Settings shared by the app and every Android library module. */
internal fun configureAndroid(android: CommonExtension) {
    android.compileSdk = COMPILE_SDK
    android.defaultConfig.minSdk = MIN_SDK
    android.defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    // Kotlin's jvmTarget follows these automatically with AGP 9's built-in Kotlin
    android.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    android.compileOptions.targetCompatibility = JavaVersion.VERSION_17
}

/** Compose libraries every Compose module needs (versions come from the BOM). */
internal fun Project.addComposeDependencies() {
    val bom = dependencies.platform(lib("androidx-compose-bom"))
    dependencies.add("implementation", bom)
    dependencies.add("implementation", lib("androidx-ui"))
    dependencies.add("implementation", lib("androidx-ui-tooling-preview"))
    dependencies.add("implementation", lib("androidx-material3"))
    dependencies.add("debugImplementation", lib("androidx-ui-tooling"))
    dependencies.add("androidTestImplementation", bom)
}
