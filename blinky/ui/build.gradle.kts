plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidFeatureConventionPlugin.kt
    alias(libs.plugins.nordic.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "no.nordicsemi.android.blinky.ui"
}

dependencies {
    implementation(project(":blinky:spec"))

    implementation(nordic.ui)
    implementation(nordic.logger)
    implementation(nordic.permissions.ble)
    implementation(nordic.log.timber)

    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.compose.material.icons.extended)
}