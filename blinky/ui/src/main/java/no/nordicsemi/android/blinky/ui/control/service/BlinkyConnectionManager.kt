package no.nordicsemi.android.blinky.ui.control.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.spec.exception.BlinkyException
import no.nordicsemi.android.log.LogContract
import timber.log.Timber

/**
 * This manager is responsible for connecting the [Blinky] device and managing its state.
 *
 * @param blinky The Blinky implementation.
 */
internal class BlinkyConnectionManager(
    private val blinky: Blinky,
) {
    /** The connection state. */
    sealed class State {
        /** Blinky is being connected to. */
        data object Connecting : State()
        /**
         * The device is ready to use.
         *
         * @param state The state allows controlling the [Blinky] device and observing its state
         * changes.
         */
        data class Ready(val state: Blinky.State): State()
        /** The device has disconnected. */
        data object Disconnected: State()
        /** The connection to the device timed out. */
        data object Timeout: State()
        /** The device is not a Blinky. */
        data object NotSupported: State()

        val blinky: Blinky.State?
            get() = (this as? Ready)?.state
    }

    private var connection: Job? = null

    /**
     * Connects to the [Blinky] device and returns a flow with the connection state changes.
     *
     * Use [disconnect] or cancel the scope in which this method is called to terminate the connection.
     * When cancelling the scope, make sure the flow continues being collected to avoid missing the
     * [State.Disconnected] state.
     *
     * @return A flow of connection states, initially [State.Connecting].
     */
    suspend fun connect() = MutableStateFlow<State>(State.Connecting)
        .also { state ->
            val scope = CoroutineScope(currentCoroutineContext())
            connection = scope.launch {
                blinky.connect(state)
            }
        }
        .asStateFlow()

    /**
     * Disconnects from the [Blinky] device.
     *
     * @return A flow of connection states, initially [State.Connecting].
     */
    fun disconnect() {
        connection?.cancel()
    }

    /**
     * Connects to the [Blinky] device and updates the [state] flow with the connection state changes.
     *
     * @param state The flow of connection states.
     */
    private suspend fun Blinky.connect(state: MutableStateFlow<State>) {
        try {
            connect { blinky ->
                state.update { State.Ready(state = blinky) }

                // Listen to the LED state changes.
                blinky.led
                    .onEach { state ->
                        when (state) {
                            true -> Timber.log(
                                LogContract.Log.Level.APPLICATION,
                                "LED turned ON"
                            )

                            false -> Timber.log(
                                LogContract.Log.Level.APPLICATION,
                                "LED turned OFF"
                            )
                        }
                    }
                    .launchIn(this)

                // Listen to the button state changes.
                blinky.button
                    .onEach { state ->
                        when (state) {
                            true -> Timber.log(
                                LogContract.Log.Level.APPLICATION,
                                "Button pressed"
                            )

                            false -> Timber.log(
                                LogContract.Log.Level.APPLICATION,
                                "Button released"
                            )
                        }
                    }
                    .launchIn(this)

                // Await scope cancellation.
                awaitCancellation()
            }
        } catch (_: BlinkyException.Timeout) {
            Timber.w("Connection timed out")
            state.update { State.Timeout }
        } catch (_: BlinkyException.ConnectionFailed) {
            Timber.w("Connection failed")
            state.update { State.Disconnected }
        } catch (_: BlinkyException.LinkLoss) {
            state.update { State.Disconnected }
        } catch (_: BlinkyException.NotSupported) {
            Timber.w("Device does not support LED Button Service (LBS)")
            state.update { State.NotSupported }
        } catch (e: CancellationException) {
            Timber.i("Connection scope cancelled")
            state.update { State.Disconnected }
            // Rethrow the cancellation exception.
            throw e
        } catch (t: Throwable) {
            Timber.e(t, "Connection error")
            state.update { State.Disconnected }
        }
    }
}