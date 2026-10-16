import com.android.build.api.dsl.ApplicationExtension
import com.harish.gallery.buildlogic.TARGET_SDK
import com.harish.gallery.buildlogic.addComposeDependencies
import com.harish.gallery.buildlogic.configureAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** `harish.android.application`: the :app module (Android app + Compose). */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<ApplicationExtension> {
                configureAndroid(this)
                defaultConfig.targetSdk = TARGET_SDK
                buildFeatures.compose = true
            }
            addComposeDependencies()
        }
    }
}
