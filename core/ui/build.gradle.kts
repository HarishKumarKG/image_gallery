plugins {
    alias(libs.plugins.harish.android.compose)
}

android {
    namespace = "com.harish.gallery.core.ui"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.lifecycle.viewmodel) // MediaSelectionViewModel
    implementation(libs.media3.exoplayer)    // VideoPlayer (viewer + Locked folder)
    implementation(libs.media3.ui)
}
