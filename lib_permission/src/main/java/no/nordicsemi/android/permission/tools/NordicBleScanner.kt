package no.nordicsemi.android.permission.tools

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter

@SuppressLint("MissingPermission")
class NordicBleScanner(private val bleAdapter: BluetoothAdapter?) {

    fun getBluetoothStatus(): ScannerStatus {
        return when {
            bleAdapter == null -> ScannerStatus.NOT_AVAILABLE
            bleAdapter.isEnabled -> ScannerStatus.ENABLED
            else -> ScannerStatus.DISABLED
        }
    }
}
