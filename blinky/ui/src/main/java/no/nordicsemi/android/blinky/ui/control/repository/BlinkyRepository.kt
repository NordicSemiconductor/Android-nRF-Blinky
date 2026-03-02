package no.nordicsemi.android.blinky.ui.control.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.spec.exception.BlinkyException
import no.nordicsemi.android.blinky.ui.control.BlinkyDevice
import no.nordicsemi.android.log.ILogSession
import no.nordicsemi.android.log.LogContract
import no.nordicsemi.android.log.timber.nRFLoggerTree
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * This repository is responsible for sending and receiving commands to the Blinky device.
 *
 * @param context The application context.
 * @param device The Blinky device.
 * @param blinky The Blinky implementation.
 */
class BlinkyRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val device: BlinkyDevice,
    private val blinky: Blinky,
): Blinky by blinky {
    companion object {
        private const val BLINK_COUNT = 5
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

    private val _buttonState = MutableStateFlow(false)
    val buttonState: StateFlow<Boolean>
        get() = _buttonState.asStateFlow()

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
                    .onStart {
                        println("AAA Repository 1.1 --- --- Led flow started")
                    }
                    .onCompletion {
                        println("AAA Repository 1.1 --- --- Led flow completed with ${it?.message}")
                    }
                    .launchIn(this)

                // Whenever user changes the LED state, update the remote device.
                ledState
                    .onEach { state ->
                        blinky.led.update { state }
                    }
                    .onStart {
                        println("AAA Repository 1.2 --- --- Led state flow started")
                    }
                    .onCompletion {
                        println("AAA Repository 1.2 --- --- Led state flow completed with ${it?.message}")
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
                        if (state)
                            throw NullPointerException("Kaczka!")
                    }
                    .onStart {
                        println("AAA Repository 1.3 --- --- Button state flow started")
                    }
                    .onCompletion {
                        println("AAA Repository 1.3 --- --- Button state flow completed with ${it?.message}")
                    }
                    .launchIn(this)

                // Blink few times.
                repeat(BLINK_COUNT) {
                    if (it > 0) {
                        delay(100.milliseconds)
                    }
                    blinky.led.update { true }
                    delay(100.milliseconds)
                    blinky.led.update { false }
                }

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
            Timber.wtf(t, "Blinky implementation error")
            _state.update { State.DISCONNECTED }
        }

        // Release resources.
        Timber.uproot(tree)
        logSession = null
    }
}