plugins {
    // https://github.com/nordicsemi/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidLibraryConventionPlugin.kt
    alias(libs.plugins.nordic.library)
    // https://github.com/nordicsemi/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidKotlinConventionPlugin.kt
    alias(libs.plugins.nordic.kotlin.android)
}

android {
    namespace = "no.nordicsemi.android.blinky.transport_ble"
}

dependencies {
    implementation(project(":blinky:spec"))

    // Import Kotlin BLE Library - core client Android module
    implementation(nordic.blek.client.core.android)
    // BLE events are logged using Timber
    implementation(libs.timber)
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
}