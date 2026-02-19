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
    implementation(nordic.ui)
    implementation(nordic.core)
    implementation(nordic.compat.scanner)
    implementation(nordic.navigation)
    implementation(nordic.permissions.ble)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
}