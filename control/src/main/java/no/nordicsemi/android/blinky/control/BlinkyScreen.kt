package no.nordicsemi.android.blinky.control

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import no.nordicsemi.android.blinky.control.viewmodel.BlinkyViewModel
import no.nordicsemi.android.common.theme.view.NordicAppBar

@Composable
fun BlinkyScreen(
    device: BluetoothDevice,
    name: String? = device.name,
    onNavigateUp: () -> Unit,
) {
    val viewModel: BlinkyViewModel = hiltViewModel()
    val ledState by viewModel.ledState.collectAsState(initial = false)
    val buttonState by viewModel.buttonState.collectAsState(initial = false)

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
            LedControlView(state = ledState, onStateChanged = {})

            Spacer(modifier = Modifier.height(16.dp))
            
            ButtonControlView(state = buttonState)
        }
    }
}