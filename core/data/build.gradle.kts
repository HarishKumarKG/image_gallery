plugins {
    alias(libs.plugins.harish.android.library)
    alias(libs.plugins.harish.hilt)
}

android {
    namespace = "com.harish.gallery.core.data"
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface) // camera details + location for the info sheet
}
