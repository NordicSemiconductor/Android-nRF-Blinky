package no.nordicsemi.android.blinky.transport_ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.ktx.*
import no.nordicsemi.android.ble.ktx.state.ConnectionState
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.spec.BlinkySpec
import no.nordicsemi.android.blinky.transport_ble.data.ButtonCallback
import no.nordicsemi.android.blinky.transport_ble.data.ButtonState
import no.nordicsemi.android.blinky.transport_ble.data.LedCallback
import no.nordicsemi.android.blinky.transport_ble.data.LedData

class BlinkyManager(
    context: Context,
    device: BluetoothDevice
): Blinky by BlinkyManagerImpl(context, device)

private class BlinkyManagerImpl(
    context: Context,
    private val device: BluetoothDevice,
): BleManager(context), Blinky {
    private val scope = CoroutineScope(Dispatchers.IO)

    private var ledCharacteristic: BluetoothGattCharacteristic? = null
    private var buttonCharacteristic: BluetoothGattCharacteristic? = null

    private val _ledState = MutableStateFlow(false)
    override val ledState = _ledState.asStateFlow()

    private val _buttonState = MutableStateFlow(false)
    override val buttonState = _buttonState.asStateFlow()

    override val state = stateAsFlow()
        .map {
            when (it) {
                is ConnectionState.Connecting,
                is ConnectionState.Initializing -> Blinky.State.LOADING
                is ConnectionState.Ready -> Blinky.State.READY
                is ConnectionState.Disconnecting,
                is ConnectionState.Disconnected -> Blinky.State.NOT_AVAILABLE
            }
        }
        .stateIn(scope, SharingStarted.Lazily, Blinky.State.NOT_AVAILABLE)

    override suspend fun connect() = connect(device)
            .retry(3, 300)
            .useAutoConnect(false)
            .timeout(2000)
            .suspend()

    override fun release() {
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
        scope.launch(exceptionHandler) {
            cancelQueue()
            disconnect().suspend()
        }
    }

    override suspend fun turnLed(state: Boolean) {
        // First, we need to write the value to the characteristic.
        writeCharacteristic(
            ledCharacteristic,
            LedData.from(state),
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        ).suspend()

        // Then, we need to read the value back to make sure it was written correctly.
        val newState = readCharacteristic(ledCharacteristic)
            .suspendForResponse<ButtonState>().state

        // Finally, we update the state flow with the new value.
        _ledState.value = newState
    }

    override fun getGattCallback(): BleManagerGattCallback {
        return BlinkyManagerGattCallback()
    }

    override fun log(priority: Int, message: String) {
        Log.println(priority, "BlinkyManager", message)
    }

    @Suppress("RedundantOverride")
    override fun getMinLogPriority(): Int {
        // By default, the library logs only INFO or
        // higher priority messages. You may change it here.
        return super.getMinLogPriority() // Log.VERBOSE
    }

    private inner class BlinkyManagerGattCallback: BleManagerGattCallback() {

        private val buttonCallback by lazy {
            object : ButtonCallback() {
                override fun onButtonStateChanged(device: BluetoothDevice, state: Boolean) {
                    _buttonState.tryEmit(state)
                }
            }
        }

        private val ledCallback by lazy {
            object : LedCallback() {
                override fun onLedStateChanged(device: BluetoothDevice, state: Boolean) {
                    _ledState.tryEmit(state)
                }
            }
        }

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            // Get the LBS Service from the gatt object.
            gatt.getService(BlinkySpec.BLINKY_SERVICE_UUID)?.apply {
                // Get the LED characteristic.
                ledCharacteristic = getCharacteristic(
                    BlinkySpec.BLINKY_LED_CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE
                )
                // Get the Button characteristic.
                buttonCharacteristic = getCharacteristic(
                    BlinkySpec.BLINKY_BUTTON_CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY
                )

                // Return true if all required characteristics are supported.
                return ledCharacteristic != null && buttonCharacteristic != null
            }
            return false
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun initialize() {
            // Enable notifications for the button characteristic.
            val flow: Flow<ButtonState> = setNotificationCallback(buttonCharacteristic)
                .asValidResponseFlow()

            // Forward the button state to the buttonState flow.
            scope.launch {
                flow.map { it.state }.collect { _buttonState.tryEmit(it) }
            }

            enableNotifications(buttonCharacteristic)
                .enqueue()

            // Read the initial value of the button characteristic.
            readCharacteristic(buttonCharacteristic)
                .with(buttonCallback)
                .enqueue()

            // Read the initial value of the LED characteristic.
            readCharacteristic(ledCharacteristic)
                .with(ledCallback)
                .enqueue()
        }

        override fun onServicesInvalidated() {
            ledCharacteristic = null
            buttonCharacteristic = null
        }
    }

}