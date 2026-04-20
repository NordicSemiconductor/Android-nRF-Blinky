plugins {
    // https://github.com/nordicsemi/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidLibraryConventionPlugin.kt
    alias(libs.plugins.nordic.library)
    // https://github.com/nordicsemi/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/AndroidKotlinConventionPlugin.kt
    alias(libs.plugins.nordic.kotlin.android)
}

android {
    namespace = "no.nordicsemi.android.blinky.spec"
}

dependencies {
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
}