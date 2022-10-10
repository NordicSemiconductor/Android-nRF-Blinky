package no.nordicsemi.android.blinky.control.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import no.nordicsemi.android.blinky.control.R
import no.nordicsemi.android.blinky.control.repository.BlinkyRepository
import no.nordicsemi.android.common.logger.NordicLogger
import no.nordicsemi.android.log.LogContract.Log
import no.nordicsemi.android.log.timber.nRFLoggerTree
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BlinkyViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    private val repository: BlinkyRepository,
) : AndroidViewModel(context as Application) {
    /** The Blinky device name, as advertised. */
    val deviceName: String
    /** The connection state of the device. */
    val state = repository.state
    /** The LED state. */
    val ledState = repository.ledState
        .map { it.also { Timber.log(Log.Level.APPLICATION, "LED state changed to $it") } }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    /** The button state. */
    val buttonState = repository.buttonState
        .map { it.also { Timber.log(Log.Level.APPLICATION, "Button state changed to $it") } }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    /** Timber tree that logs to nRF Logger. */
    private val tree: Timber.Tree
    /** If the nRF Logger is installed, this URI will allow to open the session. */
    private val sessionUri: Uri?

    init {
        // Get the navigation arguments.
        val device: BluetoothDevice = savedStateHandle["device"]!!
        val deviceName: String? = savedStateHandle["deviceName"]

        // Plant a new Tree that logs to nRF Logger.
        val key = device.address
        val name = deviceName ?: context.getString(R.string.unnamed_device)
        tree = nRFLoggerTree(context, null, key, name)
            .also { Timber.plant(it) }
            .also { sessionUri = it.session?.sessionUri }

        // Update the device name.
        this.deviceName = name

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
        NordicLogger.launch(getApplication(), sessionUri)
    }

    override fun onCleared() {
        super.onCleared()

        repository.release()
        Timber.uproot(tree)
    }
}