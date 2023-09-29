package no.nordicsemi.android.blinky.ui.control

import android.bluetooth.BluetoothDevice
import android.os.Parcelable
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.parcelize.Parcelize
import no.nordicsemi.android.blinky.ui.control.view.BlinkyScreen
import no.nordicsemi.android.common.navigation.createDestination
import no.nordicsemi.android.common.navigation.defineDestination
import no.nordicsemi.android.common.navigation.viewmodel.SimpleNavigationViewModel

val Blinky = createDestination<BlinkyDevice, Unit>("blinky")

@Parcelize
data class BlinkyDevice(
    val device: BluetoothDevice,
    val name: String?,
): Parcelable

val BlinkyDestination = defineDestination(Blinky) {
    val viewModel: SimpleNavigationViewModel = hiltViewModel()

    BlinkyScreen(
        onNavigateUp = { viewModel.navigateUp() }
    )
}
