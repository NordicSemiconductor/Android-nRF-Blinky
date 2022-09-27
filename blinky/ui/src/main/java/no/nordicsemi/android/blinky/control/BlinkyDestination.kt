package no.nordicsemi.android.blinky.control

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.common.navigation.DestinationId
import no.nordicsemi.android.common.navigation.NavigationArgument

val BlinkyDestination = DestinationId("blinky")

data class BlinkyParams(
    val device: BluetoothDevice,
    val deviceName: String?,
) : NavigationArgument {
    override val destinationId = BlinkyDestination
}