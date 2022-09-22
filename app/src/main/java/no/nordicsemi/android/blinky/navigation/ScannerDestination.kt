package no.nordicsemi.android.blinky.navigation

import no.nordicsemi.android.blinky.scanner.BlinkyScanner
import no.nordicsemi.android.common.navigation.ComposeDestination
import no.nordicsemi.android.common.navigation.ComposeDestinations
import no.nordicsemi.android.common.navigation.DestinationId

val ScannerDestination = DestinationId("scanner")

private val Scanner = ComposeDestination(ScannerDestination) { navigationManager ->
    BlinkyScanner(
        onDeviceSelected = { device, name ->
            navigationManager.navigateTo(BlinkyDestination, BlinkyParams(device, name))
        }
    )
}

val ScannerDestinations = ComposeDestinations(Scanner)