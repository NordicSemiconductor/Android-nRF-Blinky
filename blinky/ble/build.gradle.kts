plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidLibraryConventionPlugin.kt
    alias(libs.plugins.nordic.library)
}

android {
    namespace = "no.nordicsemi.android.blinky.transport_ble"
}

dependencies {
    implementation(project(":blinky:spec"))

    // Import BLE Library
    implementation(libs.nordic.ble.ktx)
    // BLE events are logged using Timber
    implementation(libs.timber)
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
}