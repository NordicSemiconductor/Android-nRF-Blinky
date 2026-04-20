plugins {
    // https://github.com/nordicsemi/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidFeatureConventionPlugin.kt
    alias(libs.plugins.nordic.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "no.nordicsemi.android.blinky.ui"
}

dependencies {
    implementation(project(":blinky:spec"))

    // Nordic common libraries.
    implementation(nordic.ui)
    implementation(nordic.logger)
    implementation(nordic.permissions.ble)
    implementation(nordic.log.timber)

    // Hilt is observing the Service lifecycle.
    implementation(libs.androidx.lifecycle.service)
    // This module contains a Nav3 entry point.
    implementation(libs.androidx.navigation3.runtime)
    // Some extra icons to make the app silky-smooth.
    implementation(libs.androidx.compose.material.icons.extended)
}