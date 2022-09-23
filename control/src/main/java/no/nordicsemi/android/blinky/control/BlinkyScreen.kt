package no.nordicsemi.android.blinky.control

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
import no.nordicsemi.android.blinky.control.view.ButtonControlView
import no.nordicsemi.android.blinky.control.view.ConnectionView
import no.nordicsemi.android.blinky.control.view.LedControlView
import no.nordicsemi.android.blinky.control.viewmodel.BlinkyViewModel
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.common.theme.view.NordicAppBar

@Composable
fun BlinkyScreen(
    onNavigateUp: () -> Unit,
) {
    val viewModel: BlinkyViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState(initial = Blinky.State.NOT_AVAILABLE)

    Column {
        NordicAppBar(
            text = viewModel.deviceName ?: stringResource(R.string.blinky_no_name),
            onNavigationButtonClick = onNavigateUp
        )
        when (state) {
            Blinky.State.READY -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    val ledState by viewModel.ledState.collectAsState(initial = false)
                    val buttonState by viewModel.buttonState.collectAsState(initial = false)

                    LedControlView(state = ledState, onStateChanged = {})

                    Spacer(modifier = Modifier.height(16.dp))

                    ButtonControlView(state = buttonState)
                }
            }
            else -> {
                ConnectionView(
                    state = state,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}