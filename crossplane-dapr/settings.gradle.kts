rootProject.name = "crossplane-dapr"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include(":common-ktor")
include(":producer-ktor")
include(":consumer-ktor")
include(":producer-springboot")
include(":consumer-springboot")
