package no.nordicsemi.android.blinky.ui.scanner.view

import android.bluetooth.BluetoothDevice
import android.os.ParcelUuid
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import no.nordicsemi.android.blinky.ui.R
import no.nordicsemi.android.scanner.DeviceSelected
import no.nordicsemi.android.scanner.ScannerScreen
import no.nordicsemi.android.blinky.spec.BlinkySpec

@Composable
fun BlinkyScanner(
    onDeviceSelected: (BluetoothDevice, String?) -> Unit,
) {
    ScannerScreen(
        title = stringResource(id = R.string.scanner_title),
        uuid = ParcelUuid(BlinkySpec.BLINKY_SERVICE_UUID),
        cancellable = false,
        onResult = { result ->
            when (result) {
                is DeviceSelected -> with(result.device) {
                    onDeviceSelected(device, name)
                }
                else -> {}
            }
        }
    )
}
