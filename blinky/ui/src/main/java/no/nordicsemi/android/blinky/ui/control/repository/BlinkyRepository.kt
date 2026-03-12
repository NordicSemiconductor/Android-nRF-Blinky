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
        const val BLINK_COUNT = 3
    }

    /** If the nRF Logger is installed, this will allow to open the session. */
    internal var logSession: ILogSession? = null

    enum class State {
        CONNECTING,
        TIMEOUT,
        READY,
        DISCONNECTED,
        NOT_SUPPORTED,
    }
    private val _state = MutableStateFlow(State.CONNECTING)
    val state: StateFlow<State>
        get() = _state.asStateFlow()

    val deviceName = device.name

    val ledState = MutableStateFlow(false)

    val blink = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val _buttonState = MutableStateFlow(false)
    val buttonState: StateFlow<Boolean>
        get() = _buttonState.asStateFlow()

    private val _buttonPressed = MutableSharedFlow<Unit>()
    val buttonPressed: SharedFlow<Unit>
        get() = _buttonPressed.asSharedFlow()

    private val _buttonLongPressed = MutableSharedFlow<Unit>()
    val buttonLongPressed: SharedFlow<Unit>
        get() = _buttonLongPressed.asSharedFlow()

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