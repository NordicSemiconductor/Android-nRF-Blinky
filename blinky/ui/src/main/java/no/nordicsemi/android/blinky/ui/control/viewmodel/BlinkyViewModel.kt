package no.nordicsemi.android.blinky.ui.control.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.blinky.ui.control.BlinkyDevice
import no.nordicsemi.android.blinky.ui.control.repository.BlinkyRepository
import no.nordicsemi.android.blinky.ui.di.BlinkyFactory
import no.nordicsemi.android.common.logger.LoggerLauncher
import timber.log.Timber

/**
 * The view model for the Blinky screen.
 *
 * @param context The application context.
 * @property repository The repository that will be used to interact with the device.
 */
@HiltViewModel(assistedFactory = BlinkyViewModel.Factory::class)
class BlinkyViewModel @AssistedInject constructor(
    @ApplicationContext context: Context,
    blinkyFactory: BlinkyFactory,
    @Assisted target: BlinkyDevice,
) : AndroidViewModel(context as Application) {
    private val repository: BlinkyRepository = BlinkyRepository(
        context = context,
        device = target,
        blinky = blinkyFactory.create(target)
    )

    @AssistedFactory
    interface Factory {
        fun create(target: BlinkyDevice): BlinkyViewModel
    }

    /** The connection state of the device. */
    val state = repository.state
    /** The device name. */
    val deviceName = repository.deviceName
    /** The LED state. */
    val ledState = repository.ledState
    /** The button state. */
    val buttonState = repository.buttonState

    /**
     * Flow of button clicks.
     *
     * A click is a sequence of button press and release events, separated by less than
     * [LONG_PRESS_TIMEOUT][no.nordicsemi.android.blinky.spec.BlinkySpec.LONG_PRESS_TIMEOUT].
     */
    val buttonPressed = repository.buttonPressed

    /**
     * Flow of long button clicks.
     *
     * A click is a sequence of button press and release events, separated by more than
     * [LONG_PRESS_TIMEOUT][no.nordicsemi.android.blinky.spec.BlinkySpec.LONG_PRESS_TIMEOUT].
     */
    val buttonLongPressed = repository.buttonLongPressed

    init {
        // In this sample we want to connect to the device as soon as the view model is created.
        connect()
    }

    /**
     * Connects to the device.
     */
    fun connect() {
        val exceptionHandler = CoroutineExceptionHandler { _, t ->
            Timber.w(t, "Connection failed with exception")
        }
        viewModelScope.launch(exceptionHandler) {
            // This method may throw an exception if the connection fails, Bluetooth is disabled, etc.
            // The exception will be caught by the exception handler and will be ignored.
            repository.connect()
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