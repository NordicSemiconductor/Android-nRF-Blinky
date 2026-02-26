package no.nordicsemi.android.blinky.ui.control.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.blinky.ui.control.repository.BlinkyRepository
import no.nordicsemi.android.common.logger.LoggerLauncher
import javax.inject.Inject

/**
 * The view model for the Blinky screen.
 *
 * @param context The application context.
 * @property repository The repository that will be used to interact with the device.
 */
@HiltViewModel
class BlinkyViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: BlinkyRepository,
) : AndroidViewModel(context as Application) {
    /** The connection state of the device. */
    val state = repository.state
    /** The device name. */
    val deviceName = repository.deviceName
    /** The LED state. */
    val ledState = repository.ledState
    /** The button state. */
    val buttonState = repository.buttonState

    init {
        // In this sample we want to connect to the device as soon as the view model is created.
        connect()
    }

    /**
     * Connects to the device.
     */
    fun connect() {
        val exceptionHandler = CoroutineExceptionHandler { _, t ->
            println("AAA VM Exception handler: ${t.message}")
        }
        viewModelScope.launch(exceptionHandler) {
            // This method may throw an exception if the connection fails,
            // Bluetooth is disabled, etc.
            // The exception will be caught by the exception handler and will be ignored.
            try {
                println("AAA VM Connect start")
                repository.connect()
                println("AAA VM Connect complete")
            } catch (e: CancellationException) {
                println("AAA VM Connect cancelled with ${e.message}")
                throw e
            } catch (e: Exception) {
                println("AAA VM Connect failed with ${e.message}")
                throw e
            }
        }
    }

    /**
     * Sends a command to the device to toggle the LED state.
     *
     * @param on The new state of the LED.
     */
    fun turnLed(on: Boolean) {
        ledState.update { on }
    }

    /**
     * Opens nRF Logger app with the log or Google Play if the app is not installed.
     */
    fun openLogger() {
        LoggerLauncher.launch(getApplication(), repository.logSession)
    }
}