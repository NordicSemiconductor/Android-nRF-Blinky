package no.nordicsemi.android.blinky.spec

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface Blinky {

    /**
     * Connects to the device.
     *
     * The method suspends for the duration of the connection. When the block is completed, the
     * connection will be closed. If the device disconnects before the block is completed, the
     * connection will be canceled throwing [CancellationException].
     *
     * @param block the block to execute when the connection is established.
     * @throws BlinkyError in case the connection fails.
     */
    suspend fun connect(block: suspend CoroutineScope.(State) -> Unit)

    /**
     * State of the Blinky device.
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
    }

}