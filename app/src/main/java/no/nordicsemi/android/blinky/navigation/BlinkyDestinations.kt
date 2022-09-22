package no.nordicsemi.android.blinky.navigation

import no.nordicsemi.android.blinky.control.BlinkyDestination
import no.nordicsemi.android.blinky.control.BlinkyScreen
import no.nordicsemi.android.common.navigation.ComposeDestination
import no.nordicsemi.android.common.navigation.ComposeDestinations

private val Blinky = ComposeDestination(BlinkyDestination) { navigationManager ->
    BlinkyScreen(
        onNavigateUp = { navigationManager.navigateUp() }
    )
}

internal val BlinkyDestinations = ComposeDestinations(Blinky)