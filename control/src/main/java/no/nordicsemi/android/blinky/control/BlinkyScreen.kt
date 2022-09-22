package no.nordicsemi.android.blinky.control

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.common.theme.view.NordicAppBar

@SuppressLint("MissingPermission")
@Composable
fun BlinkyScreen(
    device: BluetoothDevice,
    name: String? = device.name,
    onNavigateUp: () -> Unit,
) {
    //val viewModel: BlinkyViewModel by hiltViewModel()

    Column {
        NordicAppBar(
            text = name ?: stringResource(R.string.blinky_no_name),
            onNavigationButtonClick = onNavigateUp
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            LedControlView(state = true, onStateChanged = {})

            Spacer(modifier = Modifier.height(16.dp))
            
            ButtonControlView(state = false)
        }
    }
}