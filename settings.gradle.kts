pluginManagement {
    // Our own Gradle plugins (shared module setup) live in build-logic/
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Gallery"

include(":app")

// core: shared building blocks (no screens)
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":core:ui")

// features: one module per screen group; features never depend on each other
include(":feature:photos")
include(":feature:albums")
include(":feature:viewer")
include(":feature:editor")
include(":feature:search")
include(":feature:locked")
include(":feature:settings")
