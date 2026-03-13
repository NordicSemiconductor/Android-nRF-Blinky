package no.nordicsemi.android.blinky.ui.control.repository

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.spec.exception.BlinkyException
import no.nordicsemi.android.blinky.spec.toggle
import no.nordicsemi.android.blinky.ui.control.BlinkyDevice
import no.nordicsemi.android.blinky.ui.control.repository.BlinkyRepository.Companion.BLINK_COUNT
import no.nordicsemi.android.log.ILogSession
import no.nordicsemi.android.log.LogContract
import no.nordicsemi.android.log.timber.nRFLoggerTree
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

/**
 * This repository is responsible for sending and receiving commands to the Blinky device.
 *
 * @param context The application context.
 * @param device The Blinky device.
 * @param blinky The Blinky implementation.
 */
internal class BlinkyRepository(
    private val context: Context,
    private val device: BlinkyDevice,
    private val blinky: Blinky,
): Blinky by blinky {
    companion object {
        /**
         * The number of times the LED will blink.
         * @see BlinkyRepository.blink
         */
        const val BLINK_COUNT = 3
    }

    /** If the nRF Logger is installed, this will allow to open the session. */
    internal var logSession: ILogSession? = null

    /** The connection state. */
    enum class State {
        CONNECTING,
        TIMEOUT,
        READY,
        DISCONNECTED,
        NOT_SUPPORTED,
    }
    private val _state = MutableStateFlow(State.CONNECTING)
    /** The current connection state. */
    val state: StateFlow<State>
        get() = _state.asStateFlow()
    /** The device name as advertised, or read from Device Name characteristic. */
    val deviceName = device.name

    /**
     * The state of the LED on the Blinky, where `true` is turned on and `false` is turned off.
     *
     * This flow is mutable. User can set its value to turn the LED on or off.
     *
     * Note, that the change needs some time to propagate to the device. The time depends on the
     * connection interval.
     */
    val ledState = MutableStateFlow(false)

    /**
     * A flow of blink events.
     *
     * When an event is emitted on this flow, the Blinky will blink its LED [BLINK_COUNT] times.
     */
    val blink = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * The state of a binding between the Button and the LED.
     *
     * When enabled (`true`), the LED will be turned on when the button is pressed,
     * and turned off when the button is released.
     */
    var bindingState = MutableStateFlow(false)

    private val _buttonState = MutableStateFlow(false)
    /** The state of the button on the Blinky, where `true` is pressed and `false` is released. */
    val buttonState: StateFlow<Boolean>
        get() = _buttonState.asStateFlow()

    private val _buttonPressed = MutableSharedFlow<Unit>()
    /** A flow of button click events. */
    val buttonPressed: SharedFlow<Unit>
        get() = _buttonPressed.asSharedFlow()

    private val _buttonLongPressed = MutableSharedFlow<Unit>()
    /** A flow of long clicks of the button. */
    val buttonLongPressed: SharedFlow<Unit>
        get() = _buttonLongPressed.asSharedFlow()

    /**
     * Establishes and maintains a connection to the [device] using [blinky] interface.
     *
     * This method terminates on disconnection. To disconnect, cancel the job in which this
     * method is called.
     */
    suspend fun connect() {
        // Plant a new Tree that logs to nRF Logger.
        val tree = nRFLoggerTree(context, null, device.identifier, device.name)
            .also { Timber.plant(it) }
            .also { logSession = it.session }

        _state.update { State.CONNECTING }
        try {
            blinky.connect { blinky ->
                _state.update { State.READY }

                // Listen to the LED state changes.
                // Note, that this state is managed locally in `Blinky` implementation, as there are no
                // notifications for LED state.
                blinky.led
                    .onEach { state ->
                        when (state) {
                            true -> Timber.log(LogContract.Log.Level.APPLICATION, "LED turned ON")
                            false -> Timber.log(LogContract.Log.Level.APPLICATION, "LED turned OFF")
                        }
                        // Update the local LED state with the value read from the remote device.
                        ledState.update { blinky.led.value }
                    }
                    .launchIn(this)

                // Whenever user changes the LED state, update the remote device.
                ledState
                    .onEach { state ->
                        blinky.led.update { state }
                    }
                    .launchIn(this)

                // Listen to the button state changes.
                blinky.button
                    .onEach { state ->
                        when (state) {
                            true -> Timber.log(LogContract.Log.Level.APPLICATION, "Button pressed")
                            false -> Timber.log(LogContract.Log.Level.APPLICATION, "Button released")
                        }
                        _buttonState.update { state }

                        // If binding is enabled, update the LED state.
                        if (bindingState.value) {
                            blinky.led.update { state }
                        }
                    }
                    .launchIn(this)

                blinky.buttonPressed
                    .onEach { _buttonPressed.emit(Unit) }
                    .launchIn(this)

                blinky.buttonLongPressed
                    .onEach { _buttonLongPressed.emit(Unit) }
                    .launchIn(this)

                // Blink a few times when asked.
                blink
                    .onEach {
                        repeat(BLINK_COUNT * 2) {
                            blinky.led.toggle()
                            delay(500.milliseconds)
                        }
                    }
                    .launchIn(this)

                // When Button -> LED binding gets enabled, set the initial LED state
                // to the current state of the button.
                bindingState
                    .onEach { enabled ->
                        if (enabled) {
                            ledState.update { blinky.button.value }
                        }
                    }
                    .launchIn(this)

                // Now, with the states collected, await scope cancellation.
                awaitCancellation()
            }
        } catch (_: BlinkyException.Timeout) {
            Timber.w("Connection to ${device.identifier} timed out")
            _state.update { State.TIMEOUT }
        } catch (_: BlinkyException.ConnectionFailed) {
            Timber.w("Connection to ${device.identifier} failed")
            _state.update { State.DISCONNECTED }
        } catch (_: BlinkyException.LinkLoss) {
            _state.update { State.DISCONNECTED }
        } catch (_: BlinkyException.NotSupported) {
            Timber.w("${device.identifier} does not support LED Button Service (LBS)")
            _state.update { State.NOT_SUPPORTED }
        } catch (_: CancellationException) {
            Timber.i("Blinky scope cancelled")
            _state.update { State.DISCONNECTED }
        } catch (t: Throwable) {
            Timber.e(t, "Connection error")
            _state.update { State.DISCONNECTED }
        }

        // Release resources.
        Timber.uproot(tree)
        logSession = null
    }
}