package no.nordicsemi.android.blinky.scanner

import androidx.core.os.bundleOf
import no.nordicsemi.android.blinky.scanner.view.BlinkyScanner
import no.nordicsemi.android.common.navigation.DestinationId
import no.nordicsemi.android.common.navigation.NavigationDestination
import no.nordicsemi.android.common.navigation.NavigationDestinations

val ScannerDestination = DestinationId("scanner")

private val Scanner = NavigationDestination(ScannerDestination) { navigator ->
    BlinkyScanner(
        onDeviceSelected = { device, name ->
            navigator.navigate(
                hint = null,
                args = bundleOf(
                    "device" to device,
                    "deviceName" to name,
                )
            )
        }
    )
}

val ScannerDestinations = NavigationDestinations(Scanner)