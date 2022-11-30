plugins {
    alias(libs.plugins.nordic.feature)
    id("kotlin-parcelize")
}

android {
    namespace = "no.nordicsemi.android.blinky.control"
}

dependencies {
    implementation(project(":blinky:spec"))

    implementation(libs.nordic.theme)
    implementation(libs.nordic.uilogger)
    implementation(libs.nordic.uiscanner)
    implementation(libs.nordic.navigation)
    implementation(libs.nordic.permission)

    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.nordic.log.timber)
}