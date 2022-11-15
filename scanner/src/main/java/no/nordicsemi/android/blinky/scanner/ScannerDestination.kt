package no.nordicsemi.android.blinky.scanner

import androidx.hilt.navigation.compose.hiltViewModel
import no.nordicsemi.android.blinky.control.Blinky
import no.nordicsemi.android.blinky.control.BlinkyDevice
import no.nordicsemi.android.blinky.scanner.view.BlinkyScanner
import no.nordicsemi.android.common.navigation.createSimpleDestination
import no.nordicsemi.android.common.navigation.defineDestination
import no.nordicsemi.android.common.navigation.viewmodel.SimpleNavigationViewModel

val Scanner = createSimpleDestination("scanner")

val ScannerDestination = defineDestination(Scanner) {
    val viewModel: SimpleNavigationViewModel = hiltViewModel()

    BlinkyScanner(
        onDeviceSelected = { device, name ->
            viewModel.navigateTo(Blinky, BlinkyDevice(device, name))
        }
    )
}