plugins {
    // https://github.com/nordicsemi/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidApplicationComposeConventionPlugin.kt
    alias(libs.plugins.nordic.application.compose)
    // https://github.com/nordicsemi/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidHiltConventionPlugin.kt
    alias(libs.plugins.nordic.hilt)
}

android {
    namespace = "no.nordicsemi.android.blinky"
    defaultConfig {
        applicationId = "no.nordicsemi.android.nrfblinky"
    }
    @Suppress("UnstableApiUsage")
    androidResources {
        localeFilters += listOf("en")
    }
    flavorDimensions += listOf("mode")
    productFlavors {
        create("native") {
            isDefault = true
            dimension = "mode"
        }
        create("mock") {
            dimension = "mode"
        }
    }
}

dependencies {
    implementation(project(":scanner"))
    implementation(project(":blinky:spec"))
    implementation(project(":blinky:ui"))
    implementation(project(":blinky:ble"))

    // Applies the Nordic theme and the splash screen.
    implementation(nordic.theme)

    // Choose the Bluetooth LE client implementation, depending on the flavor.
    // See: https://github.com/nordicsemi/Kotlin-BLE-Library
    "nativeImplementation"(nordic.blek.client.android)
    "mockImplementation"(nordic.blek.client.android.mock)
    // Provide the LocalEnvironmentOwner for Composables.
    // This allows to mock permission requests in mock environment.
    implementation(nordic.blek.environment.android.compose)

    // AndroidX dependencies required by the :app module.
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewModel.navigation3)
    // Logging framework.
    implementation(libs.timber)
    // Adds a bridge SLF4J -> Timber.
    implementation(libs.slf4j.timber)
    // Leak Canary lib allows to easily find memory leaks in the app.
    debugImplementation(libs.leakcanary)
}