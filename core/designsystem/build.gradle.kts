plugins {
    alias(libs.plugins.harish.android.compose)
}

android {
    namespace = "com.harish.gallery.core.designsystem"
}

dependencies {
    api(libs.androidx.material3)
    implementation(libs.androidx.core.ktx)
}
