package no.nordicsemi.android.blinky.navigation

import no.nordicsemi.android.blinky.control.BlinkyDestination
import no.nordicsemi.android.blinky.control.BlinkyParams
import no.nordicsemi.android.blinky.scanner.BlinkyScanner
import no.nordicsemi.android.blinky.scanner.ScannerDestination
import no.nordicsemi.android.common.navigation.ComposeDestination
import no.nordicsemi.android.common.navigation.ComposeDestinations

private val Scanner = ComposeDestination(ScannerDestination) { navigationManager ->
    BlinkyScanner(
        onDeviceSelected = { device, name ->
            navigationManager.navigateTo(BlinkyDestination, BlinkyParams(device, name))
        }
    )
}

internal val ScannerDestinations = ComposeDestinations(Scanner)