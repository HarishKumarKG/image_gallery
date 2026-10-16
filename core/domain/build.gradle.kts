plugins {
    alias(libs.plugins.harish.jvm.library)
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject) // @Inject on use cases, so Hilt can create them

    testImplementation(libs.kotlinx.coroutines.test)
}
