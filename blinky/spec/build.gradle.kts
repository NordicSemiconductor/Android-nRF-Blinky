plugins {
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidLibraryConventionPlugin.kt
    alias(libs.plugins.nordic.library)
}

android {
    namespace = "no.nordicsemi.android.blinky.spec"
}

dependencies {
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
}