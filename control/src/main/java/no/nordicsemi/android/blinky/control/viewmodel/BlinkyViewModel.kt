package no.nordicsemi.android.blinky.control.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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

    val ledState = repository.ledState

    val buttonState = repository.buttonState

    val state = repository.state

    val deviceName: String?

    init {
        val parameters = navigationManager.getArgument(BlinkyDestination) as BlinkyParams

        deviceName = parameters.deviceName

        viewModelScope.launch(Dispatchers.IO) {
            repository.connect()
        }
    }

    override fun onCleared() {
        super.onCleared()

        repository.release()
    }
}