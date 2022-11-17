plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.nordic.application)
    alias(libs.plugins.nordic.application.compose)
    alias(libs.plugins.nordic.hilt)
}

android {
    namespace = "no.nordicsemi.android.blinky"
    defaultConfig {
        resConfigs("en")
    }
}

dependencies {
    implementation(project(":scanner"))
    implementation(project(":blinky:spec"))
    implementation(project(":blinky:ui"))
    implementation(project(":blinky:ble"))

    implementation(libs.nordic.theme)
    implementation(libs.nordic.navigation)

    implementation(libs.timber)

    implementation(libs.androidx.activity.compose)
}