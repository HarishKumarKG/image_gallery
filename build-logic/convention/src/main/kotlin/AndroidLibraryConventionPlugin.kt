import com.android.build.api.dsl.LibraryExtension
import com.harish.gallery.buildlogic.configureAndroid
import com.harish.gallery.buildlogic.lib
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** `harish.android.library`: an Android library without UI (e.g. :core:data). */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                configureAndroid(this)
                // Unit tests can call Android classes without crashing (they return defaults)
                testOptions.unitTests.isReturnDefaultValues = true
            }
            dependencies.add("testImplementation", lib("junit"))
        }
    }
}
