package no.nordicsemi.android.blinky.control.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nordicsemi.android.blinky.control.BlinkyDestination
import no.nordicsemi.android.blinky.control.BlinkyParams
import no.nordicsemi.android.blinky.control.repository.BlinkyRepository
import no.nordicsemi.android.common.navigation.NavigationManager
import javax.inject.Inject

@HiltViewModel
class BlinkyViewModel @Inject constructor(
    private val repository: BlinkyRepository,
    navigationManager: NavigationManager,
) : ViewModel() {
    val state = repository.state
    val ledState = repository.ledState
    val buttonState = repository.buttonState

    /** The Blinky device name, as advertised. */
    val deviceName: String?

    init {
        val parameters = navigationManager.getArgument(BlinkyDestination) as BlinkyParams

        deviceName = parameters.deviceName

        reconnect()
    }

    fun reconnect() {
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            repository.connect()
        }
    }

    fun toggleLed(state: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.turnLed(state)
        }
    }

    override fun onCleared() {
        super.onCleared()

        repository.release()
    }
}