package no.nordicsemi.android.blinky.control

import no.nordicsemi.android.blinky.control.view.BlinkyScreen
import no.nordicsemi.android.common.navigation.DestinationId
import no.nordicsemi.android.common.navigation.NavigationDestination
import no.nordicsemi.android.common.navigation.NavigationDestinations

val BlinkyDestination = DestinationId("blinky")

private val Blinky = NavigationDestination(BlinkyDestination) { navigator ->
    BlinkyScreen(
        onNavigateUp = { navigator.navigateUp() },
    )
}

val BlinkyDestinations = NavigationDestinations(Blinky)