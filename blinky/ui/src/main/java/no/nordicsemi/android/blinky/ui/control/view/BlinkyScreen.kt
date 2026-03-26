package no.nordicsemi.android.blinky.ui.control.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.blinky.ui.R
import no.nordicsemi.android.blinky.ui.control.BlinkyDevice
import no.nordicsemi.android.blinky.ui.control.service.BlinkyConnectionManager
import no.nordicsemi.android.blinky.ui.control.service.BlinkyService
import no.nordicsemi.android.blinky.ui.control.viewmodel.BlinkyViewModel
import no.nordicsemi.android.blinky.ui.view.DeviceConnectingView
import no.nordicsemi.android.blinky.ui.view.DeviceDisconnectedView
import no.nordicsemi.android.blinky.ui.view.Reason
import no.nordicsemi.android.common.logger.view.LoggerAppBarIcon
import no.nordicsemi.android.common.permissions.ble.RequireBluetooth
import no.nordicsemi.android.common.ui.view.NordicAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BlinkyScreen(
    device: BlinkyDevice,
    onNavigateUp: () -> Unit,
) {
    val viewModel = hiltViewModel<BlinkyViewModel, BlinkyViewModel.Factory> { factory ->
        factory.create(device)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler {
        // Stopping the service will disconnect the devices.
        BlinkyService.stop(context)

        onNavigateUp()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NordicAppBar(
            title = { Text(text = viewModel.deviceName ?: stringResource(R.string.unnamed_device)) },
            onNavigationButtonClick = {
                // Stopping the service will disconnect the devices.
                BlinkyService.stop(context)
                
                onNavigateUp()
            },
            actions = {
                LoggerAppBarIcon(onClick = viewModel::openLogger)
            }
        )
        RequireBluetooth {
            when (val s = state) {
                BlinkyConnectionManager.State.Connecting -> {
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
                BlinkyConnectionManager.State.Timeout -> {
                    DeviceDisconnectedView(
                        reason = Reason.TIMEOUT,
                        modifier = Modifier.padding(16.dp),
                    ) { padding ->
                        Button(
                            onClick = viewModel::connect,
                            modifier = Modifier.padding(padding),
                        ) {
                            Text(text = stringResource(id = R.string.action_retry))
                        }
                    }
                }
                BlinkyConnectionManager.State.Disconnected -> {
                    DeviceDisconnectedView(
                        reason = Reason.LINK_LOSS,
                        modifier = Modifier.padding(16.dp),
                    ) { padding ->
                        Button(
                            onClick = viewModel::connect,
                            modifier = Modifier.padding(padding),
                        ) {
                            Text(text = stringResource(id = R.string.action_retry))
                        }
                    }
                }
                BlinkyConnectionManager.State.NotSupported -> {
                    DeviceDisconnectedView(
                        reason = Reason.MISSING_SERVICE,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                is BlinkyConnectionManager.State.Ready -> {
                    val ledState by s.state.led.collectAsStateWithLifecycle()
                    val buttonState by s.state.button.collectAsStateWithLifecycle()
                    val bindingState by viewModel.bindingState.collectAsStateWithLifecycle()

                    BlinkyControlView(
                        ledState = ledState,
                        onStateChanged = viewModel::turnLed,
                        onBlink = viewModel::blinkLed,
                        bindingState = bindingState,
                        onBindingChanged = { viewModel.bindingState.value = it },
                        buttonState = buttonState,
                        buttonPressed = s.state.buttonPressed,
                        buttonLongPressed = s.state.buttonLongPressed,
                        modifier = Modifier
                            .widthIn(max = 460.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}