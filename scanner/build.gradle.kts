plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidFeatureConventionPlugin.kt
    alias(libs.plugins.nordic.feature)
    // https://developer.android.com/kotlin/parcelize
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "no.nordicsemi.android.scanner"
}

dependencies {
    implementation(libs.nordic.ui)
    implementation(libs.nordic.core)
    implementation(libs.nordic.compat.scanner)
    implementation(libs.nordic.navigation)
    implementation(libs.nordic.permissions.ble)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
}