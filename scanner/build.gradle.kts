plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidFeatureConventionPlugin.kt
    alias(libs.plugins.nordic.feature)
}

android {
    namespace = "no.nordicsemi.android.blinky.scanner"
}

dependencies {
    implementation(project(":blinky:spec"))
    implementation(project(":blinky:ui"))

    implementation(libs.nordic.uiscanner)
    implementation(libs.nordic.navigation)
}