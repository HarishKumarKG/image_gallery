// A small separate build that only contains our "convention plugins":
// each one sets up a kind of module (app, library, feature...) in one line.
dependencyResolutionManagement {
    repositories {
        google()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
