package no.nordicsemi.android.blinky.ui.control

import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.blinky.ui.control.service.BlinkyService
import no.nordicsemi.android.blinky.ui.control.view.BlinkyScreen

@Serializable
data class BlinkyDevice(
    val identifier: String,
    val name: String?,
)

@Serializable
data class BlinkyKey(val device: BlinkyDevice): NavKey

fun EntryProviderScope<NavKey>.blinkyEntry(
    onNavigateUp: () -> Unit
) = entry<BlinkyKey> { key ->
    val context = LocalContext.current

    BlinkyScreen(
        device = key.device,
        onNavigateUp = {
            // Stopping the service will disconnect the devices.
            BlinkyService.stop(context)

            onNavigateUp()
       },
    )
}