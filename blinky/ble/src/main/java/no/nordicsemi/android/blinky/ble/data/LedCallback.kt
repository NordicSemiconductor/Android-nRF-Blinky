package no.nordicsemi.android.blinky.ble.data

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.callback.profile.ProfileReadResponse
import no.nordicsemi.android.ble.data.Data

abstract class LedCallback: ProfileReadResponse() {

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        if (data.size() == 1) {
            val ledState = data.getIntValue(Data.FORMAT_UINT8, 0) == 0x01
            onLedStateChanged(device, ledState)
        } else {
            onInvalidDataReceived(device, data)
        }
    }

    abstract fun onLedStateChanged(device: BluetoothDevice, state: Boolean)
}