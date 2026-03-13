package no.nordicsemi.android.blinky.spec

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import no.nordicsemi.android.blinky.spec.exception.BlinkyException

interface Blinky {

    /**
     * Connects to the Blinky device and passes over the control to the given [block].
     *
     * This method completes when the device gets disconnected.
     *
     * If the scope, in which this method is called, is canceled, the connection will be closed.
     *
     * @param block The profile implementation.
     * @throws BlinkyException.ConnectionFailed When connection to the peripheral fails.
     * @throws BlinkyException.Timeout When connection to the peripheral times out.
     * @throws BlinkyException.NotSupported When the peripheral does not support the required LED Button Service (LBS).
     * @throws BlinkyException.LinkLoss When the connection to the peripheral is lost.
     */
    suspend fun connect(block: suspend CoroutineScope.(State) -> Unit)

    /**
     * The state of the Blinky device.
     *
     * This interface represents the state of the Blinky device. An implementation of this
     * interface can be obtained from [Blinky.connect] method.
     */
    interface State {

        /**
         * The current state of the LED.
         *
         * Set the [value][MutableStateFlow.value] to change the LED state.
         */
        val led: MutableStateFlow<Boolean>

        /**
         * The current state of the button.
         *
         * This flow emits `true` when the button is pressed and `false` when it is released.
         */
        val button: StateFlow<Boolean>

        /**
         * The flow of button click events.
         *
         * This flow emits an event when the button is clicked.
         */
        val buttonPressed: Flow<Unit>

        /**
         * The flow of long button clicks events.
         *
         * This flow emits an event when the button is pressed for 2 seconds.
         */
        val buttonLongPressed: Flow<Unit>
    }

}

/**
 * Toggles the LED state.
 */
fun MutableStateFlow<Boolean>.toggle() {
    value = !value
}