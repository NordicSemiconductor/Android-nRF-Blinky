package no.nordicsemi.android.blinky.scanner

import android.bluetooth.BluetoothDevice
import android.os.ParcelUuid
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import no.nordicsemi.android.blinky.spec.R
import no.nordicsemi.android.blinky.spec.BlinkySpec
import no.nordicsemi.android.common.ui.scanner.DeviceSelected
import no.nordicsemi.android.common.ui.scanner.ScannerScreen

@Composable
fun BlinkyScanner(
    onDeviceSelected: (BluetoothDevice, String?) -> Unit,
) {
    ScannerScreen(
        title = stringResource(id = R.string.app_name),
        uuid = ParcelUuid(BlinkySpec.BLINKY_SERVICE_UUID),
        cancellable = false,
        onResult = { result ->
            when (result) {
                is DeviceSelected -> onDeviceSelected(result.device.device, result.device.name)
                else -> {}
            }
        }
    )
}
