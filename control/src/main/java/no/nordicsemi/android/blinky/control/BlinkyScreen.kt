package no.nordicsemi.android.blinky.control

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import no.nordicsemi.android.blinky.control.view.ButtonControlView
import no.nordicsemi.android.blinky.control.view.ConnectingView
import no.nordicsemi.android.blinky.control.view.DisconnectedView
import no.nordicsemi.android.blinky.control.view.LedControlView
import no.nordicsemi.android.blinky.control.viewmodel.BlinkyViewModel
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.common.logger.view.LoggerAppBarIcon
import no.nordicsemi.android.common.permission.RequireBluetooth
import no.nordicsemi.android.common.theme.view.NordicAppBar

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
                    ConnectingView(
                        modifier = Modifier.fillMaxSize()
                    )
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
                    DisconnectedView(
                        modifier = Modifier.fillMaxSize(),
                        onReconnect = { viewModel.connect() }
                    )
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