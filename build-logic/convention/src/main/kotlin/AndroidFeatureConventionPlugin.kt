import com.harish.gallery.buildlogic.lib
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * `harish.android.feature`: a screen module (photos, albums, viewer...).
 * Compose + Hilt + the core modules + ViewModel/navigation, all in one line.
 * Feature modules never depend on each other; the :app module connects them.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("harish.android.compose")
            pluginManager.apply("harish.hilt")

            listOf(":core:model", ":core:domain", ":core:data", ":core:designsystem", ":core:ui").forEach {
                dependencies.add("implementation", dependencies.project(mapOf("path" to it)))
            }
            dependencies.add("implementation", lib("androidx-activity-compose")) // BackHandler, activity results
            dependencies.add("implementation", lib("androidx-lifecycle-runtime-compose"))
            dependencies.add("implementation", lib("androidx-lifecycle-viewmodel"))
            dependencies.add("implementation", lib("androidx-navigation-compose"))
            dependencies.add("implementation", lib("androidx-hilt-lifecycle-viewmodel-compose"))
        }
    }
}
