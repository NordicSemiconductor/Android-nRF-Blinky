package no.nordicsemi.android.blinky.navigation

import android.bluetooth.BluetoothDevice
import androidx.compose.material3.Text
import no.nordicsemi.android.common.navigation.ComposeDestination
import no.nordicsemi.android.common.navigation.ComposeDestinations
import no.nordicsemi.android.common.navigation.DestinationId
import no.nordicsemi.android.common.navigation.NavigationArgument

val BlinkyDestination = DestinationId("blinky")

private val Blinky = ComposeDestination(BlinkyDestination) { navigationManager ->
    val device = navigationManager.getArgument(BlinkyDestination) as BlinkyParams
    Text(text = device.device.name)
}

val BlinkyDestinations = ComposeDestinations(Blinky)

data class BlinkyParams(val device: BluetoothDevice) : NavigationArgument {
    override val destinationId = BlinkyDestination
}