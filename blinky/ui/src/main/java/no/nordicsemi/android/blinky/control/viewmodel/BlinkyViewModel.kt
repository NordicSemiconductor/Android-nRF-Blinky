package no.nordicsemi.android.blinky.control.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import no.nordicsemi.android.blinky.control.repository.BlinkyRepository
import no.nordicsemi.android.common.logger.NordicLogger
import javax.inject.Inject
import javax.inject.Named

/**
 * The view model for the Blinky screen.
 *
 * @param context The application context.
 * @property repository The repository that will be used to interact with the device.
 * @property deviceName The name of the Blinky device, as advertised.
 */
@HiltViewModel
class BlinkyViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: BlinkyRepository,
    @Named("deviceName") val deviceName: String,
) : AndroidViewModel(context as Application) {
    /** The connection state of the device. */
    val state = repository.state
    /** The LED state. */
    val ledState = repository.loggedLedState
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    /** The button state. */
    val buttonState = repository.loggedButtonState
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    init {
        connect()
    }

    fun connect() {
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.connect()
        }
    }

    fun toggleLed(state: Boolean) {
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.turnLed(state)
        }
    }

    fun openLogger() {
        NordicLogger.launch(getApplication(), repository.sessionUri)
    }

    override fun onCleared() {
        super.onCleared()
        repository.release()
    }
}