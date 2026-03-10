plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidApplicationComposeConventionPlugin.kt
    alias(libs.plugins.nordic.application.compose)
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidHiltConventionPlugin.kt
    alias(libs.plugins.nordic.hilt)
}

android {
    namespace = "no.nordicsemi.android.blinky"
    defaultConfig {
        applicationId = "no.nordicsemi.android.nrfblinky"
        resourceConfigurations.add("en")
    }
}

dependencies {
    implementation(project(":scanner"))
    implementation(project(":blinky:spec"))
    implementation(project(":blinky:ui"))
    implementation(project(":blinky:ble"))

    implementation(nordic.theme)
    // Choose the client implementation, depending on the flavor
    implementation(nordic.blek.client.android)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewModel.navigation3)

    implementation(libs.timber)
    implementation(libs.slf4j.timber)
}