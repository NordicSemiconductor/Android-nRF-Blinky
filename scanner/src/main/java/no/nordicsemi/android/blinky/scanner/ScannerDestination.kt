package no.nordicsemi.android.blinky.scanner

import androidx.core.os.bundleOf
import no.nordicsemi.android.blinky.scanner.view.BlinkyScanner
import no.nordicsemi.android.common.navigation.asDestinations
import no.nordicsemi.android.common.navigation.createDestination
import no.nordicsemi.android.common.navigation.defineDestination

val Scanner = createDestination("scanner")

private val ScannerDestination = defineDestination(Scanner) { navigator, _ ->
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

val ScannerDestinations = ScannerDestination.asDestinations()