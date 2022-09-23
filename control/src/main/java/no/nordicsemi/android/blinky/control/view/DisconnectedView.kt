package no.nordicsemi.android.blinky.control.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.blinky.control.R

@Composable
fun DisconnectedView(
    modifier: Modifier = Modifier,
    onReconnect: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = stringResource(id = R.string.blinky_state_disconnected))
        
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onReconnect) {
            Text(text = stringResource(id = R.string.blinky_action_retry))
        }
    }
}

@Preview
@Composable
fun DisconnectedViewPreview() {
    DisconnectedView(
        onReconnect = {}
    )
}