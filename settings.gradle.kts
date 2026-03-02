pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
    versionCatalogs {
        // Link: https://github.com/NordicSemiconductor/Nordic-Gradle-Plugins
        create("libs") {
            from("no.nordicsemi.android.gradle:version-catalog:2.14-1")
        }
        // Link: https://github.com/NordicSemiconductor/Nordic-Version-Catalog
        create("nordic") {
            from("no.nordicsemi.android:version-catalog:2026.02.00")
        }
    }
}
rootProject.name = "nRF Blinky"

include(":app")
include(":blinky:spec")
include(":blinky:ui")
include(":blinky:ble")

// Clone https://github.com/NordicPlayground/Android-Common-Libraries and
// uncomment the following lines to modify source code of the Nordic Common library:
//if (file("../Android-Common-Libraries").exists()) {
//    includeBuild("../Android-Common-Libraries")
//}

// Clone https://github.com/NordicSemiconductor/Kotlin-BLE-Library and
// uncomment the following lines to modify source code of the BLE library:
//if (file("../Kotlin-BLE-Library").exists()) {
//    includeBuild("../Kotlin-BLE-Library")
//}
