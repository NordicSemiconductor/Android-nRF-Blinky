plugins {
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