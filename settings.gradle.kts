pluginManagement {
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
        maven(url = "https://jitpack.io")
        // 下载到本地使用
        maven(url = uri("local_mavenrepo"))
        // 直接使用 github 上的仓库
//        maven(url = "https://raw.github.com/GrayLand119/ecgi_ring_sdk/master/local_mavenrepo")
    }
}

rootProject.name = "ECG SDK Demo Public"
include(":app")

