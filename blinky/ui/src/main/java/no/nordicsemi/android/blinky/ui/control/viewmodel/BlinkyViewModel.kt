package no.nordicsemi.android.blinky.ui.control.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.media.AudioAttributes
import android.os.Build
import android.os.CombinedVibration
import android.os.IBinder
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.blinky.ui.control.BlinkyDevice
import no.nordicsemi.android.blinky.ui.control.service.BlinkyConnectionManager
import no.nordicsemi.android.blinky.ui.control.service.BlinkyService
import no.nordicsemi.android.blinky.ui.control.util.blink
import no.nordicsemi.android.common.logger.LoggerLauncher
import no.nordicsemi.android.log.ILogSession
import timber.log.Timber

@HiltViewModel(assistedFactory = BlinkyViewModel.Factory::class)
internal class BlinkyViewModel @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted private val target: BlinkyDevice,
) : AndroidViewModel(context as Application) {

    companion object {
        const val BLINK_COUNT = 3
    }

    @AssistedFactory
    interface Factory {
        fun create(target: BlinkyDevice): BlinkyViewModel
    }

    /** The current state of the connection. */
    val state = MutableStateFlow<BlinkyConnectionManager.State>(BlinkyConnectionManager.State.Connecting)
    /**
     * The current state of the Button -> LED binding.
     *
     * With the binding enabled, the LED state will be updated when the button state changes.
     */
    val bindingState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    /** The device name. */
    val deviceName = target.name

    private val serviceConnection = object : ServiceConnection {
        var logSession: StateFlow<ILogSession?>? = null
        private var job: Job? = null

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BlinkyService.LocalBinder
            logSession = binder.logSession

            // Set the initial binding state to the one from the service.
            // This is necessary to restore the binding state after the ViewModel (and Activity)
            // was recreated and bound to an existing Service.
            bindingState.update { binder.bindingState.value }

            // Start collecting connection state changes.
            job = viewModelScope.launch {
                // The UI is the source of the binding changes.
                // Its state needs to be passed to the Service. The Service is responsible
                // for controlling the LED state on Button state changes if binding is enabled.
                bindingState
                    // Skip initial state. Otherwise, when the ViewModel is recreated and binds to
                    // an existing Service, the initial state (false) would be emitted overwriting
                    // the actual state set by the user.
                    .drop(1)
                    .onEach { isBound ->
                        binder.bindingState.update { isBound }
                    }
                    .launchIn(this)

                // Handle connection state changes from the service.
                binder.state
                    .onEach { newState ->
                        state.update { newState }
                    }
                    // Handle vibrations from the repository flows.
                    .filterIsInstance<BlinkyConnectionManager.State.Ready>()
                    .onEach { blinky ->
                        blinky.state.button
                            .onEach {
                                try {
                                    vibrate(context, false)
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to vibrate")
                                }
                            }
                            .launchIn(this)

                        blinky.state.buttonLongPressed
                            .onEach {
                                try {
                                    vibrate(context, true)
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to vibrate")
                                }
                            }
                            .launchIn(this)
                    }
                    .collect()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // Stop collecting Service events.
            job?.cancel()
            logSession = null
        }
    }

    init {
        BlinkyService.bind(context, serviceConnection)
        connect()
    }

    override fun onCleared() {
        super.onCleared()
        // Unbind, but keep the Service running.
        // The Service will be stopped by the BackHandler if the user navigates back to
        // the Scanner screen (see BlinkyScreen).
        // The ViewModel may be cleared also when the app gets stopped while in background.
        // In that case we intend to keep the Service running and handling the connection.
        BlinkyService.unbind(getApplication(), serviceConnection)
    }

    // View Model public API

    fun connect() {
        BlinkyService.start(getApplication(), target)
    }

    fun turnLed(on: Boolean) {
        state.value.blinky?.led?.value = on
    }

    fun blinkLed() {
        viewModelScope.launch {
            state.value.blinky?.blink(BLINK_COUNT)
        }
    }

    fun openLogger() {
        LoggerLauncher.launch(getApplication(), serviceConnection.logSession?.value)
    }

    // Helper methods

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
        vm.vibrate(CombinedVibration.createParallel(effect), attributes)
    }

    private fun vibrateLegacy(context: Context, longClick: Boolean) {
        val durationMs = if (longClick) 400L else 50L
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
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