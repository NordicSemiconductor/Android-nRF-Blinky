plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidFeatureConventionPlugin.kt
    alias(libs.plugins.nordic.feature)
}

android {
    namespace = "no.nordicsemi.android.scanner"
}

dependencies {
    implementation(nordic.scanner.ble)

    implementation(libs.androidx.navigation3.runtime)
}