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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.mozilla.org/maven2/")
            content {
                includeGroup("org.mozilla.geckoview")
            }
        }
        maven {
            url = uri("https://jitpack.io")
            content {
                excludeGroup("org.mozilla.geckoview")
            }
        }
    }
}

rootProject.name = "com.absolutemango.htmlgom"
include(":backdrop")
include(":app")
include(":androidApp")
include(":app:htmlgom")
include(":editor")
project(":editor").projectDir = file("app/sampledata/editor")


include(":htmlgow")
include(":expressive-ui")
