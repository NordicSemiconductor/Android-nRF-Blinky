package no.nordicsemi.android.scanner.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import no.nordicsemi.android.common.scanner.DeviceSelected
import no.nordicsemi.android.common.scanner.ScannerScreen
import no.nordicsemi.android.common.scanner.data.OnlyNearby
import no.nordicsemi.android.common.scanner.data.OnlyWithNames
import no.nordicsemi.android.common.scanner.data.WithServiceUuid
import no.nordicsemi.android.common.scanner.rememberFilterState
import no.nordicsemi.android.scanner.R
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The scanner screen.
 *
 * @param onDeviceSelected The callback that is called when a device is selected.
 * The parameters are the device identifier (MAC) and the device name (if available).
 */
@OptIn(ExperimentalUuidApi::class)
@Composable
internal fun BlinkyScanner(
    onDeviceSelected: (String, String?) -> Unit,
) {
    // The scanner uses a Nordic common component (scanner-ble) from
    // https://github.com/nordicsemi/Android-Common-Libraries
    ScannerScreen(
        title = {
            Text(stringResource(id = R.string.scanner_title))
        },
        state = rememberFilterState(
            dynamicFilters = listOf(
                OnlyNearby(),
                OnlyWithNames(isInitiallySelected = true),
                WithServiceUuid(
                    uuid = Uuid.parse("00001523-1212-efde-1523-785feabcd123"),
                    isInitiallySelected = true
                )
            )
        ),
        cancellable = false,
        onResultSelected = { result ->
            when (result) {
                is DeviceSelected -> with(result.scanResult) {
                    onDeviceSelected(peripheral.identifier, advertisingData.name ?: peripheral.name)
                }
                else -> {}
            }
        },
    )
}
