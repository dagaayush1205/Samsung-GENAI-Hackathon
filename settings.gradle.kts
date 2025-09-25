pluginManagement {
        repositories {
            google()
            mavenCentral()
            gradlePluginPortal()
            maven { url = uri("https://developer.samsung.com/repo") }
        }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("[https://developer.samsung.com/repo](https://developer.samsung.com/repo)") }
        mavenLocal()
    }
}



rootProject.name = "SamsungHackathon"
include(":app")
