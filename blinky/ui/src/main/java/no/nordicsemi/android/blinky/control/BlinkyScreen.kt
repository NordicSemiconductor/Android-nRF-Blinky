package no.nordicsemi.android.blinky.control

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import no.nordicsemi.android.blinky.control.view.ButtonControlView
import no.nordicsemi.android.blinky.control.view.LedControlView
import no.nordicsemi.android.blinky.control.viewmodel.BlinkyViewModel
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.common.logger.view.LoggerAppBarIcon
import no.nordicsemi.android.common.permission.RequireBluetooth
import no.nordicsemi.android.common.theme.view.NordicAppBar
import no.nordicsemi.android.common.ui.scanner.view.DeviceConnectingView
import no.nordicsemi.android.common.ui.scanner.view.DeviceDisconnectedView
import no.nordicsemi.android.common.ui.scanner.view.Reason

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlinkyScreen(
    onNavigateUp: () -> Unit,
) {
    val viewModel: BlinkyViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    Column {
        NordicAppBar(
            text = viewModel.deviceName,
            onNavigationButtonClick = onNavigateUp,
            actions = {
                LoggerAppBarIcon(onClick = { viewModel.openLogger() })
            }
        )
        RequireBluetooth(scanning = false) {
            when (state) {
                Blinky.State.LOADING -> {
                    DeviceConnectingView(
                        modifier = Modifier.padding(16.dp),
                    ) { padding ->
                        Button(
                            onClick = onNavigateUp,
                            modifier = Modifier.padding(padding),
                        ) {
                            Text(text = stringResource(id = R.string.action_cancel))
                        }
                    }
                }
                Blinky.State.READY -> {
                    val ledState by viewModel.ledState.collectAsState()
                    val buttonState by viewModel.buttonState.collectAsState()

                    BlinkyControl(
                        ledState = ledState,
                        buttonState = buttonState,
                        onStateChanged = { viewModel.toggleLed(it) }
                    )
                }
                Blinky.State.NOT_AVAILABLE -> {
                    DeviceDisconnectedView(
                        reason = Reason.LINK_LOSS,
                        modifier = Modifier.padding(16.dp),
                    ) { padding ->
                        Button(
                            onClick = { viewModel.connect() },
                            modifier = Modifier.padding(padding),
                        ) {
                            Text(text = stringResource(id = R.string.action_retry))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlinkyControl(
    ledState: Boolean,
    buttonState: Boolean,
    onStateChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        LedControlView(
            state = ledState,
            onStateChanged = onStateChanged
        )

        Spacer(modifier = Modifier.height(16.dp))

        ButtonControlView(
            state = buttonState
        )
    }
}

@Preview
@Composable
fun BlinkyControlPreview() {
    BlinkyControl(
        ledState = true,
        buttonState = true,
        onStateChanged = {}
    )
}