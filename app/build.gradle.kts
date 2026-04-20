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

        @Suppress("UnstableApiUsage")
        androidResources {
            localeFilters += listOf("en")
        }
    }
}

dependencies {
    implementation(project(":scanner"))
    implementation(project(":blinky:spec"))
    implementation(project(":blinky:ui"))
    implementation(project(":blinky:ble"))

    // Choose the client implementation, depending on the flavor.
    implementation(nordic.blek.client.android)
    // Applies the Nordic theme and the splash screen.
    implementation(nordic.theme)
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