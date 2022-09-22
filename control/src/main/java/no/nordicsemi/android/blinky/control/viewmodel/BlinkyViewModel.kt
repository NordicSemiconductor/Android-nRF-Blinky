package no.nordicsemi.android.blinky.control.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import no.nordicsemi.android.blinky.control.state.ConnectionState
import javax.inject.Inject

@HiltViewModel
class BlinkyViewModel @Inject constructor() : ViewModel() {

    private val _ledState = MutableStateFlow(false)
    val ledState: Flow<Boolean> = _ledState

    private val _buttonState = MutableStateFlow(true)
    val buttonState: Flow<Boolean> = _buttonState

    val state: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Connecting)

}