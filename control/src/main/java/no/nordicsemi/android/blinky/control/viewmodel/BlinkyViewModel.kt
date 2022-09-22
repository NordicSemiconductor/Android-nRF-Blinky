package no.nordicsemi.android.blinky.control.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import no.nordicsemi.android.blinky.control.BlinkyDestination
import no.nordicsemi.android.blinky.control.BlinkyParams
import no.nordicsemi.android.blinky.control.repository.BlinkyRepository
import no.nordicsemi.android.blinky.control.state.ConnectionState
import no.nordicsemi.android.common.navigation.NavigationManager
import javax.inject.Inject

@HiltViewModel
class BlinkyViewModel @Inject constructor(
    val repository: BlinkyRepository,
    navigationManager: NavigationManager,
) : ViewModel() {

    private val _ledState = MutableStateFlow(false)
    val ledState: Flow<Boolean> = _ledState

    private val _buttonState = MutableStateFlow(true)
    val buttonState: Flow<Boolean> = _buttonState

    val state: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Connecting)

    val deviceName: String?

    init {
        val parameters = navigationManager.getArgument(BlinkyDestination) as BlinkyParams

        this.deviceName = parameters.deviceName
    }

}