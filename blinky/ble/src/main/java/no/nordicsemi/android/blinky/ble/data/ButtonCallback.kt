package no.nordicsemi.android.blinky.ble.data

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.callback.profile.ProfileReadResponse
import no.nordicsemi.android.ble.data.Data

abstract class ButtonCallback: ProfileReadResponse() {

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        if (data.size() == 1) {
            val buttonState = data.getIntValue(Data.FORMAT_UINT8, 0) == 0x01
            onButtonStateChanged(device, buttonState)
        } else {
            onInvalidDataReceived(device, data)
        }
    }

    abstract fun onButtonStateChanged(device: BluetoothDevice, state: Boolean)
}