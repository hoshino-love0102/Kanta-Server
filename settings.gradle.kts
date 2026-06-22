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

rootProject.name = "kanta-server"

include("service-kanban")
include("service-user")
include("service-auth")
include("service-gateway")
include("service-workspace")
include("service-meeting")
