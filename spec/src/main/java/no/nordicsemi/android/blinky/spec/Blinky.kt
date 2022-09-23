package no.nordicsemi.android.blinky.spec

import kotlinx.coroutines.flow.Flow

interface Blinky {

    enum class State {
        LOADING,
        READY,
        NOT_AVAILABLE
    }

    /**
     * Connects to the device.
     */
    suspend fun connect()

    /**
     * Disconnects from the device.
     */
    fun release()

    /**
     * The current state of the blinky.
     */
    val state: Flow<State>

    /**
     * The current state of the LED.
     */
    val ledState: Flow<Boolean>

    /**
     * The current state of the button.
     */
    val buttonState: Flow<Boolean>

    /**
     * Controls the LED state.
     *
     * @param state the new state of the LED.
     */
    suspend fun turnLed(state: Boolean)
}