plugins {
    alias(libs.plugins.nordic.library)
}

android {
    namespace = "no.nordicsemi.android.blinky.spec"
}

dependencies {
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
}