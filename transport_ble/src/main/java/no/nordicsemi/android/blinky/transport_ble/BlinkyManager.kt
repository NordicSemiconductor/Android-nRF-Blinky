package no.nordicsemi.android.blinky.transport_ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.ktx.asValidResponseFlow
import no.nordicsemi.android.ble.ktx.getCharacteristic
import no.nordicsemi.android.ble.ktx.state.ConnectionState
import no.nordicsemi.android.ble.ktx.stateAsFlow
import no.nordicsemi.android.ble.ktx.suspend
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.spec.BlinkySpec
import no.nordicsemi.android.blinky.transport_ble.data.ButtonState
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

    private val _ledState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val ledState = _ledState.asSharedFlow()

    private val _buttonState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val buttonState = _buttonState.asStateFlow()

    override val state = stateAsFlow().map {
        when (it) {
            is ConnectionState.Connecting,
            is ConnectionState.Initializing -> Blinky.State.LOADING
            is ConnectionState.Ready -> Blinky.State.READY
            is ConnectionState.Disconnecting,
            is ConnectionState.Disconnected -> Blinky.State.NOT_AVAILABLE
        }
    }

    override suspend fun connect() = connect(device)
            .retry(3, 300)
            .useAutoConnect(false)
            .timeout(2000)
            .suspend()

    override fun release() {
        scope.launch {
            cancelQueue()
            disconnect().suspend()
        }
    }

    override suspend fun turnLed(state: Boolean) {
        writeCharacteristic(
            ledCharacteristic,
            LedData.from(state),
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        ).suspend()
    }

    override fun getGattCallback(): BleManagerGattCallback {
        return BlinkyManagerGattCallback()
    }

    private inner class BlinkyManagerGattCallback: BleManagerGattCallback() {

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
            val flow: Flow<ButtonState> = setNotificationCallback(buttonCharacteristic)
                .asValidResponseFlow()

            scope.launch {
                flow.map { it.state }.collect { _buttonState.tryEmit(it) }
            }

            enableNotifications(buttonCharacteristic)
                .enqueue()
//            readCharacteristic(buttonCharacteristic)
//                .with(object : ButtonCallback() {
//                    override fun onButtonStateChanged(device: BluetoothDevice, state: Boolean) {
//                        _buttonState.tryEmit(state)
//                    }
//                })
//                .enqueue()
        }

        override fun onServicesInvalidated() {
            ledCharacteristic = null
            buttonCharacteristic = null
        }
    }

}