package no.nordicsemi.android.blinky.ble

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.spec.BlinkySpec
import no.nordicsemi.android.blinky.spec.exception.BlinkyException
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.client.exception.InvalidAttributeException
import no.nordicsemi.kotlin.ble.core.ConnectionState.Disconnected.Reason
import kotlin.uuid.ExperimentalUuidApi

/**
 * A Bluetooth LE implementation of the [Blinky] interface.
 *
 * The manager is responsible for setting up the Bluetooth LE connection to the peripheral
 * with the LED Button Service (LBS), which becomes available once the connection is established.
 *
 * Use [connect] to initiate the connection to the peripheral.
 * @param centralManager The central manager to use to connect to the peripheral.
 * @param peripheral The peripheral.
 */
@OptIn(ExperimentalUuidApi::class)
class BlinkyManager(
    private val centralManager: CentralManager,
    private val peripheral: Peripheral,
): Blinky {

    override suspend fun connect(
        block: suspend CoroutineScope.(Blinky.State) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        // First, install the LBS profile.
        //
        // Note, that in this implementation the profile is installed before creating the connection,
        // but these can be swapped.
        peripheral.profile(
            serviceUuid = BlinkySpec.SERVICE_UUID,
            required = true,
        ) { remoteService ->
            val ledButtonService = LedButtonServiceImpl(remoteService, this)

            // Give the user control over the Blinky.
            try {
                block(ledButtonService)
            } catch (e: CancellationException) {
                // Don't disconnect when services were invalidated.
                if (e.cause !is InvalidAttributeException) {
                    peripheral.disconnect()
                }
                throw e
            }
        }

        // Initiate connection, if not connected already.
        try {
            centralManager.connect(peripheral)
        } catch (_: TimeoutCancellationException) {
            throw BlinkyException.Timeout()
        } catch (_: Exception) {
            throw BlinkyException.ConnectionFailed()
        }

        // Keep the coroutine alive until the peripheral disconnects.
        // This method returns the disconnection reason.
        val reason = peripheral.awaitDisconnection()
        if (reason == Reason.RequiredServiceNotFound) {
            throw BlinkyException.NotSupported()
        }
        if (reason != Reason.Success) {
            throw BlinkyException.LinkLoss()
        }
    }
}