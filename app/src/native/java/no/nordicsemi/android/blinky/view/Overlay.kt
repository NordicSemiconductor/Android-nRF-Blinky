package no.nordicsemi.android.blinky.view

import androidx.compose.runtime.Composable

@Composable
fun Overlay(
    content: @Composable () -> Unit
) {
    // Do nothing. This is implemented in "mock" flavor only.
    content()
}