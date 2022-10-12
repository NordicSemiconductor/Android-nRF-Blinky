package no.nordicsemi.android.blinky.control

import no.nordicsemi.android.blinky.control.view.BlinkyScreen
import no.nordicsemi.android.common.navigation.asDestinations
import no.nordicsemi.android.common.navigation.defineDestination
import no.nordicsemi.android.common.navigation.createDestination

val Blinky = createDestination("blinky")

private val BlinkyDestination = defineDestination(Blinky) { navigator ->
    BlinkyScreen(
        onNavigateUp = { navigator.navigateUp() },
    )
}

val BlinkyDestinations = BlinkyDestination.asDestinations()
