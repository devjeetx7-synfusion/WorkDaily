pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral {
            url = uri("https://repo1.maven.org/maven2/")
        }
        gradlePluginPortal()
    }
}

rootProject.name = "WorkDaily-Admin"
include(":app")
