plugins {
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