plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidFeatureConventionPlugin.kt
    alias(libs.plugins.nordic.feature)
    // https://developer.android.com/kotlin/parcelize
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "no.nordicsemi.android.blinky.ui"
}

dependencies {
    implementation(project(":blinky:spec"))

    implementation(nordic.ui)
    implementation(nordic.logger)
    implementation(nordic.scanner.ble)
    implementation(nordic.permissions.ble)
    implementation(nordic.log.timber)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.compose.material.icons.extended)
}