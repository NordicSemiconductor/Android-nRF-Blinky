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
import no.nordicsemi.android.blinky.spec.ConnectionFailed
import no.nordicsemi.android.blinky.spec.LinkLoss
import no.nordicsemi.android.blinky.spec.NotSupported
import no.nordicsemi.android.blinky.spec.Timeout
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
                        println("AAA BR 1.1. Led flow started")
                    }
                    .onCompletion {
                        println("AAA BR 1.1. Led flow completed with ${it?.message}")
                    }
                    .launchIn(this)

                // Whenever user changes the LED state, update the remote device.
                ledState
                    .onEach { state ->
                        blinky.led.update { state }
                    }
                    .onStart {
                        println("AAA BR 1.2. Led state flow started")
                    }
                    .onCompletion {
                        println("AAA BR 1.2. Led state flow completed with ${it?.message}")
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
                    .onStart {
                        println("AAA BR 1.3. Button state flow started")
                    }
                    .onCompletion {
                        println("AAA BR 1.3. Button state flow completed with ${it?.message}")
                    }
                    .launchIn(this)

                // Blink 3 times
                repeat(3) {
                    delay(200.milliseconds)
                    blinky.led.update { true }
                    delay(200.milliseconds)
                    blinky.led.update { false }
                }

                // Now, with the states collected, let's await scope cancellation.
                try {
                    awaitCancellation()
                } catch (e: CancellationException) {
                    println("AAA BR User block was canceled with ${e.message}")
                    throw e
                } catch (e: Exception) {
                    println("AAA BR User block failed with ${e.message}")
                    throw e
                } finally {
                    println("AAA BR User block canceled")
                    _state.update { State.DISCONNECTED }
                }
            }
        } catch (_: Timeout) {
            println("AAA BR connect timed out")
            _state.update { State.TIMEOUT }
        } catch (_: ConnectionFailed) {
            println("AAA BR connection failed")
            _state.update { State.DISCONNECTED }
        } catch (_: LinkLoss) {
            println("AAA BR link loss")
            _state.update { State.DISCONNECTED }
        } catch (_: NotSupported) {
            println("AAA BR not supported")
            _state.update { State.NOT_SUPPORTED }
        } catch (e: CancellationException) {
            println("AAA BR connect was canceled with ${e.message}")
            _state.update { State.DISCONNECTED }
        } catch (e: Exception) {
            println("AAA BR connect failed with ${e.message}")
            _state.update { State.DISCONNECTED }
        }

        // Release resources.
        Timber.uproot(tree)
        logSession = null
    }
}