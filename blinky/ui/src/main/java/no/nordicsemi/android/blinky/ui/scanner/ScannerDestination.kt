package no.nordicsemi.android.blinky.ui.scanner

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.blinky.ui.control.BlinkyDevice
import no.nordicsemi.android.blinky.ui.scanner.view.BlinkyScanner

@Serializable
data object ScannerKey: NavKey

fun EntryProviderScope<NavKey>.scannerEntry(
    onDeviceSelected: (BlinkyDevice) -> Unit
) = entry<ScannerKey> {
    BlinkyScanner(
        onDeviceSelected = { device, name ->
            onDeviceSelected(BlinkyDevice(device, name))
        }
    )
}