package no.nordicsemi.android.blinky.ui.control.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
internal class BlinkyViewModel @AssistedInject constructor(
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
     * The state of a binding between the Button and the LED.
     *
     * When enabled (`true`), the LED will be turned on when the button is pressed,
     * and turned off when the button is released.
     */
    val bindingState = repository.bindingState

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
        // Vibrate shortly on button press and release events.
        buttonState
            .onEach { 
                try {
                    vibrate(context, false)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to vibrate")
                }
            }
            .launchIn(viewModelScope)

        // Vibrate long on button long press events.
        buttonLongPressed
            .onEach { 
                try {
                    vibrate(context, true)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to vibrate")
                }
            }
            .launchIn(viewModelScope)

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
     * Sends an event to the repository to start blinking LED [BlinkyRepository.BLINK_COUNT] times.
     */
    fun blinkLed() {
        repository.blink.tryEmit(Unit)
    }


    /**
     * Opens nRF Logger app with the log or Google Play if the app is not installed.
     */
    fun openLogger() {
        LoggerLauncher.launch(getApplication(), repository.logSession)
    }

    // Private helper API

    private fun vibrate(context: Context, longClick: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibrateApi31(context, longClick)
        } else {
            vibrateLegacy(context, longClick)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun vibrateApi31(context: Context, longClick: Boolean) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager

        val attributes = VibrationAttributes.Builder()
            .setUsage(VibrationAttributes.USAGE_NOTIFICATION)
            .build()

        val effect = if (longClick) {
            VibrationEffect.createOneShot(400L, VibrationEffect.DEFAULT_AMPLITUDE)
        } else {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        }

        // On API 31+, use VibratorManager with CombinedVibration and VibrationAttributes
        vm.vibrate(CombinedVibration.createParallel(effect), attributes)
    }

    private fun vibrateLegacy(context: Context, longClick: Boolean) {
        val durationMs = if (longClick) 400L else 50L

        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = if (longClick) {
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            } else {
                VibrationEffect.createOneShot(durationMs, 50)
            }

            // On API 26-32, use AudioAttributes for haptic hints
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, audioAttributes)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }
}