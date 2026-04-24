package no.nordicsemi.android.blinky.view

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import no.nordicsemi.android.blinky.R
import no.nordicsemi.android.blinky.di.blinky
import no.nordicsemi.android.blinky.di.blinkyState
import no.nordicsemi.kotlin.ble.client.mock.PeripheralSpec
import no.nordicsemi.kotlin.ble.client.mock.Proximity
import no.nordicsemi.kotlin.ble.core.Manager
import no.nordicsemi.kotlin.ble.environment.android.compose.LocalEnvironmentOwner
import no.nordicsemi.kotlin.ble.environment.android.mock.MockAndroidEnvironment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Overlay(
    content: @Composable () -> Unit,
) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    )
    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContainerColor = MaterialTheme.colorScheme.errorContainer,
        sheetPeekHeight = 0.dp,
        sheetContent = {
            MockControls()
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.BottomEnd,
        ) {
            content()
            Box(
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.navigationBars
                        .union(WindowInsets.displayCutout)
                        .union(WindowInsets(left = 16.dp, right = 16.dp, bottom = 32.dp))
                )
            ) {
                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    onClick = {
                        scope.launch {
                            scaffoldState.bottomSheetState.expand()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_frame_bug_24),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun MockControls() {
    val environment = LocalEnvironmentOwner.current as MockAndroidEnvironment

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars
                    .union(WindowInsets(bottom = 16.dp))
            )
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.mock_overlay_title),
            style = MaterialTheme.typography.titleLarge,
        )
        MockBluetoothControl(environment)
        MockControl(blinky) {
            val isLedOn by blinkyState.led.collectAsStateWithLifecycle()
            TableRow(
                title = stringResource(R.string.mock_controls_control)
            ) {
                DevKitLed(on = isLedOn)
                DevKitButton(
                    onStateChanged = { blinkyState.buttonState = it },
                )
            }
        }
    }
}

@Composable
private fun MockBluetoothControl(environment: MockAndroidEnvironment) {
    val state by environment.bluetoothState.collectAsStateWithLifecycle()

    TableRow(
        title = stringResource(R.string.mock_controls_bluetooth)
    ) {
        Switch(
            checked = state == Manager.State.POWERED_ON,
            onCheckedChange = { newState ->
                if (newState) {
                    environment.simulatePowerOn()
                } else {
                    environment.simulatePowerOff()
                }
            }
        )
    }
}

@Composable
private fun MockControl(
    spec: PeripheralSpec<String>,
    custom: @Composable ColumnScope.() -> Unit = {},
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.2f),
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row {
                Icon(
                    painter = painterResource(R.drawable.outline_developer_board_24),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = spec.name ?: stringResource(R.string.mock_unknown_device),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            TableRow(
                title = stringResource(R.string.mock_controls_power)
            ) {
                var on by remember { mutableStateOf(spec.proximity != Proximity.OUT_OF_RANGE) }
                Switch(
                    checked = on,
                    onCheckedChange = { newState ->
                        on = newState
                        if (newState) {
                            spec.simulateProximityChange(Proximity.NEAR)
                        } else {
                            spec.simulateProximityChange(Proximity.OUT_OF_RANGE)
                        }
                    }
                )
                Button(
                    onClick = spec::simulateReset,
                ) {
                    Text(text = stringResource(R.string.mock_controls_power_reset))
                }
            }
            custom()
        }
    }
}

@Composable
private fun TableRow(
    title: String,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title)
        Spacer(modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
private fun DevKitLed(on: Boolean) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .background(if (on) Color.Green else Color.Gray.copy(alpha = 0.5f))
    )
}

@Composable
private fun DevKitButton(
    onStateChanged: (Boolean) -> Unit = {},
) {
    // This should look like a normal Button, but instead it supports onPress and onRelease.
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .semantics { role = Role.Button }
            .pointerInput(onStateChanged) {
                detectTapGestures(
                    onPress = {
                        onStateChanged(true)
                        tryAwaitRelease()
                        onStateChanged(false)
                    }
                )
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .defaultMinSize(
                    minWidth = ButtonDefaults.MinWidth,
                    minHeight = ButtonDefaults.MinHeight,
                )
                .padding(ButtonDefaults.ContentPadding)
        ) {
            Text(text = stringResource(R.string.mock_controls_control_button))
        }
    }
}