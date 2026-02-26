package no.nordicsemi.android.blinky.ble

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.spec.BlinkySpec
import no.nordicsemi.android.blinky.spec.ConnectionFailed
import no.nordicsemi.android.blinky.spec.LinkLoss
import no.nordicsemi.android.blinky.spec.NotSupported
import no.nordicsemi.android.blinky.spec.Timeout
import no.nordicsemi.kotlin.ble.client.RemoteServices
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class BlinkyManager(
    private val centralManager: CentralManager,
    private val peripheral: Peripheral,
): Blinky {

    override suspend fun connect(
        block: suspend CoroutineScope.(Blinky.State) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        var userJob: Job? = null

        // First, subscribe for services with the filter set to LBS Service.
        //
        // Note, that this will not initiate connection to the device. This is done below using
        // the connect() method.
        //
        // The services flow will initially emit "Unknown" (as the services are not discovered yet).
        // Upon successful connection the flow will emit "Discovering" followed by:
        // 1. Discovered - when service discovery was successful.
        // 2. Unknown - when the device disconnected before service discovery finished.
        // 3. Failed - when service discovery failed, giving the reason as a parameter.
        //
        // Note, that disconnection will transition the state to Unknown, not to Failed.
        // This is to ensure, that whenever the device is disconnected, the services state is the same.
        peripheral.services(listOf(BlinkySpec.BLINKY_SERVICE_UUID))
            .onEach { state ->
                when (state) {
                    is RemoteServices.Unknown -> {
                        // A running user job means that the device has disconnected while user was
                        // interacting with it.
                        userJob?.let {  userJob ->
                            println("AAA BM Services invalidated, cancelling")
                            cancel(CancellationException(LinkLoss()))
                            return@onEach
                        }
                    }
                    is RemoteServices.Discovered -> {
                        // When service discovery finished, we should get a list with 0 or more services.
                        // 0 services means that the device does not have LBS service, in which case
                        // an exception will be thrown.
                        // The LBS sample (https://docs.nordicsemi.com/bundle/ncs-latest/page/nrf/samples/bluetooth/peripheral_lbs/README.html)
                        // has 1 instance of the LED Button service.
                        // In case the sample was modified to support multiple instances, this
                        // implementation will only use the first one.
                        try {
                            val remoteService = requireNotNull(state.services.firstOrNull()) {
                                throw NotSupported()
                            }

                            // Start a coroutine to handle user block.
                            userJob = launch {
                                // This will throw if the service does not have the required characteristics
                                // or the characteristics have unexpected properties.
                                val ledButtonService = LedButtonServiceImpl(remoteService, this)

                                // Give user the control over the Blinky.
                                try {
                                    println("AAA BM user block started")
                                    block(ledButtonService)
                                } catch (e: CancellationException) {
                                    println("AAA BM user block canceled with ${e.message}")
                                    throw e
                                } catch (e: Exception) {
                                    println("AAA BM user block failed with ${e.message}")
                                    throw e
                                } finally {
                                    println("AAA BM user block complete")
                                    userJob = null

                                    // We can cancel the connection at that point.
                                    this@withContext.cancel("Connection finally canceled")
                                }
                            }
                        } catch (e: Exception) {
                            println("AAA BM catch ${e.message}")
                            cancel(CancellationException(e))
                        }
                    }
                    is RemoteServices.Failed -> {
                        println("AAA BM Service discovery failed with ${state.reason}")
                        cancel(CancellationException(ConnectionFailed()))
                    }
                    else -> { /* Ignore */ }
                }
            }
            .onStart {
                println("AAA BM 1. services flow started")
            }
            .onCompletion {
                println("AAA BM 1. services flow completed")
            }
            .launchIn(this)

        // Initiate connection, if not connected already.
        try {
            centralManager.connect(peripheral)
        } catch (e: TimeoutCancellationException) {
            throw Timeout()
        } catch (e: Exception) {
            throw ConnectionFailed()
        }

        // Wait until user job is complete, the device has disconnected or the device has no LBS service.
        try {
            println("AAA BM awaiting cancellation")
            awaitCancellation()
        } catch (e: CancellationException) {
            println("AAA BM cancelled with ${e.message}, cause: ${e.cause?.message}")
            throw e.cause ?: e
        } catch (e: Exception) {
            println("AAA BM failed with ${e.message}")
            throw e
        } finally {
            println("AAA BM disconnecting")
            withContext(NonCancellable) {
                // Disconnect when done.
                peripheral.disconnect()
                println("AAA BM disconnected")
            }
        }
    }
}