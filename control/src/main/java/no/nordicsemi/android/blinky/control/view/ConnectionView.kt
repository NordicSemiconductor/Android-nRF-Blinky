package no.nordicsemi.android.blinky.control.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.blinky.control.state.ConnectionState

@Composable
fun ConnectionView(
    state: ConnectionState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = state.toText())
    }
}

@Preview
@Composable
fun ConnectionViewPreview() {
    ConnectionView(state = ConnectionState.Connecting)
}

private fun ConnectionState.toText(): String = when (this) {
    ConnectionState.Connecting -> "Connecting..."
    ConnectionState.Initializing -> "Initializing..."
    ConnectionState.Connected -> "Connected"
    is ConnectionState.Disconnected -> "Disconnected\n$reason"
}