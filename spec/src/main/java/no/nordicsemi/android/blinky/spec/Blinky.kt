package no.nordicsemi.android.blinky.spec

import kotlinx.coroutines.flow.SharedFlow

interface Blinky {
    /**
     * Connects to the device.
     */
    suspend fun connect()

    /**
     * Disconnects from the device.
     */
    suspend fun release()

    /**
     * The current state of the LED.
     */
    val ledState: SharedFlow<Boolean>

    /**
     * The current state of the button.
     */
    val buttonState: SharedFlow<Boolean>

    /**
     * Controls the LED state.
     *
     * @param state the new state of the LED.
     */
    suspend fun turnLed(state: Boolean)
}