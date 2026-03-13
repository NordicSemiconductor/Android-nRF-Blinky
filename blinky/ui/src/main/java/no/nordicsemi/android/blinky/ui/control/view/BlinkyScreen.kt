package no.nordicsemi.android.blinky.ui.control.view

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.blinky.ui.R
import no.nordicsemi.android.blinky.ui.control.BlinkyKey
import no.nordicsemi.android.blinky.ui.control.repository.BlinkyRepository
import no.nordicsemi.android.blinky.ui.control.viewmodel.BlinkyViewModel
import no.nordicsemi.android.blinky.ui.state.view.DeviceConnectingView
import no.nordicsemi.android.blinky.ui.state.view.DeviceDisconnectedView
import no.nordicsemi.android.blinky.ui.state.view.Reason
import no.nordicsemi.android.common.logger.view.LoggerAppBarIcon
import no.nordicsemi.android.common.permissions.ble.RequireBluetooth
import no.nordicsemi.android.common.ui.view.NordicAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BlinkyScreen(
    key: BlinkyKey,
    onNavigateUp: () -> Unit,
) {
    val viewModel = hiltViewModel<BlinkyViewModel, BlinkyViewModel.Factory> { factory ->
        factory.create(key.device)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NordicAppBar(
            title = { Text(text = viewModel.deviceName ?: stringResource(R.string.unnamed_device)) },
            onNavigationButtonClick = onNavigateUp,
            actions = {
                LoggerAppBarIcon(onClick = { viewModel.openLogger() })
            }
        )
        RequireBluetooth {
            when (state) {
                BlinkyRepository.State.CONNECTING -> {
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
                BlinkyRepository.State.TIMEOUT -> {
                    DeviceDisconnectedView(
                        reason = Reason.TIMEOUT,
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
                BlinkyRepository.State.DISCONNECTED -> {
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
                BlinkyRepository.State.NOT_SUPPORTED -> {
                    DeviceDisconnectedView(
                        reason = Reason.MISSING_SERVICE,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                BlinkyRepository.State.READY -> {
                    val ledState by viewModel.ledState.collectAsStateWithLifecycle()
                    val buttonState by viewModel.buttonState.collectAsStateWithLifecycle()
                    val bindingState by viewModel.bindingState.collectAsStateWithLifecycle()

                    BlinkyControlView(
                        ledState = ledState,
                        onStateChanged = { viewModel.turnLed(it) },
                        onBlink = { viewModel.blinkLed() },
                        bindingState = bindingState,
                        onBindingChanged = { viewModel.bindingState.value = it },
                        buttonState = buttonState,
                        buttonPressed = viewModel.buttonPressed,
                        buttonLongPressed = viewModel.buttonLongPressed,
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