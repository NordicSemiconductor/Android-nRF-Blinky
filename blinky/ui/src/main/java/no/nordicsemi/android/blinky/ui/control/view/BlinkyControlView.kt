package no.nordicsemi.android.blinky.ui.control.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
internal fun BlinkyControlView(
    ledState: Boolean,
    onStateChanged: (Boolean) -> Unit,
    onBlink: () -> Unit,
    bindingState: Boolean,
    onBindingChanged: (Boolean) -> Unit,
    buttonState: Boolean,
    buttonPressed: Flow<Unit>,
    buttonLongPressed: Flow<Unit>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LedControlView(
            state = ledState,
            enabled = !bindingState,
            onStateChanged = onStateChanged,
            onBlink = onBlink,
        )

        Binding(
            on = bindingState,
            onBindingChanged = onBindingChanged,
        )

        ButtonControlView(
            state = buttonState,
            buttonPressed = buttonPressed,
            buttonLongPressed = buttonLongPressed,
        )
    }
}

@Composable
private fun Binding(
    on: Boolean,
    onBindingChanged: (Boolean) -> Unit,
    connectionHeight: Dp = 16.dp,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VerticalDivider(
            modifier = Modifier.height(connectionHeight)
        )

        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 40.dp) {
            OutlinedIconToggleButton(
                checked = on,
                onCheckedChange = onBindingChanged,
                border = if (on) null else BorderStroke(1.dp, DividerDefaults.color),
                modifier = Modifier.defaultMinSize(minHeight = 1.dp),
            ) {
                Icon(
                    imageVector = if (on) Icons.Default.Link else Icons.Default.LinkOff,
                    contentDescription = null,
                )
            }
        }

        VerticalDivider(
            modifier = Modifier.height(connectionHeight)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BlinkyControlViewPreview() {
    var bindingState by rememberSaveable { mutableStateOf(false) }

    BlinkyControlView(
        ledState = true,
        onStateChanged = {},
        onBlink = {},
        bindingState = bindingState,
        onBindingChanged = { bindingState = it },
        buttonState = true,
        buttonPressed = emptyFlow(),
        buttonLongPressed = emptyFlow(),
        modifier = Modifier.padding(16.dp),
    )
}