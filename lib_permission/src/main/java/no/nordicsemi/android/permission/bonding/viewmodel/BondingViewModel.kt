package no.nordicsemi.android.permission.bonding.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.permission.bonding.repository.BondingStateObserver
import no.nordicsemi.android.theme.viewmodel.CloseableViewModel
import no.nordicsemi.android.utils.BondingState
import no.nordicsemi.android.utils.SelectedBluetoothDeviceHolder

class BondingViewModel(
    private val deviceHolder: SelectedBluetoothDeviceHolder,
    private val bondingStateObserver: BondingStateObserver
) : CloseableViewModel() {

    val state = MutableStateFlow(deviceHolder.getBondingState())

    init {
        bondingStateObserver.events.onEach { event ->
            event.device?.let {
                if (it == deviceHolder.device) {
                    state.tryEmit(event.bondState)
                } else {
                    state.tryEmit(BondingState.NONE)
                }
            } ?: state.tryEmit(event.bondState)
        }.launchIn(viewModelScope)
        bondingStateObserver.startObserving()
    }

    fun bondDevice() {
        deviceHolder.bondDevice()
    }

    override fun onCleared() {
        super.onCleared()
        bondingStateObserver.stopObserving()
    }
}
