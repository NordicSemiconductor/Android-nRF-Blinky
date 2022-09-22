package no.nordicsemi.android.blinky.navigation

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.blinky.control.BlinkyScreen
import no.nordicsemi.android.common.navigation.ComposeDestination
import no.nordicsemi.android.common.navigation.ComposeDestinations
import no.nordicsemi.android.common.navigation.DestinationId
import no.nordicsemi.android.common.navigation.NavigationArgument

val BlinkyDestination = DestinationId("blinky")

private val Blinky = ComposeDestination(BlinkyDestination) { navigationManager ->
    val parameters = navigationManager.getArgument(BlinkyDestination) as BlinkyParams
    BlinkyScreen(
        device = parameters.device,
        name = parameters.deviceName,
        onNavigateUp = { navigationManager.navigateUp() }
    )
}

val BlinkyDestinations = ComposeDestinations(Blinky)

data class BlinkyParams(
    val device: BluetoothDevice,
    val deviceName: String?,
) : NavigationArgument {
    override val destinationId = BlinkyDestination
}