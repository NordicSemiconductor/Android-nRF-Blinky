package no.nordicsemi.android.blinky.scanner

import android.bluetooth.BluetoothDevice
import android.os.ParcelUuid
import androidx.compose.runtime.Composable
import no.nordicsemi.android.common.ui.scanner.DeviceSelected
import no.nordicsemi.android.common.ui.scanner.ScannerScreen
import java.util.*

@Composable
fun BlinkyScanner(
    onDeviceSelected: (BluetoothDevice) -> Unit,
) {
    ScannerScreen(
        uuid = ParcelUuid(UUID.fromString("00001523-1212-efde-1523-785feabcd123")), // TODO externalize
        allowBack = false,
        onResult = { result ->
            when (result) {
                is DeviceSelected -> onDeviceSelected(result.device.device)
                else -> {}
            }
        }
    )
}
