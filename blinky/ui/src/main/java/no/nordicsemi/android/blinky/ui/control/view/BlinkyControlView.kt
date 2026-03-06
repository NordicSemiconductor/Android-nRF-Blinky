package no.nordicsemi.android.blinky.ui.control.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
internal fun BlinkyControlView(
    ledState: Boolean,
    buttonState: Boolean,
    buttonPressed: Flow<Unit>,
    buttonLongPressed: Flow<Unit>,
    onStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LedControlView(
            state = ledState,
            onStateChanged = onStateChanged,
        )

        ButtonControlView(
            state = buttonState,
            buttonPressed = buttonPressed,
            buttonLongPressed = buttonLongPressed,
        )
    }
}

@Preview
@Composable
private fun BlinkyControlViewPreview() {
    BlinkyControlView(
        ledState = true,
        buttonState = true,
        buttonPressed = emptyFlow(),
        buttonLongPressed = emptyFlow(),
        onStateChanged = {},
        modifier = Modifier.padding(16.dp),
    )
}