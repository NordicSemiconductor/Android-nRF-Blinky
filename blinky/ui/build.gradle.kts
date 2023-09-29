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
    implementation(project(":scanner"))

    implementation(libs.nordic.theme)
    implementation(libs.nordic.logger)
    implementation(libs.nordic.uilogger)
    implementation(libs.nordic.navigation)
    implementation(libs.nordic.permissions.ble)
    implementation(libs.nordic.log.timber)

    implementation(libs.androidx.compose.material.iconsExtended)
}