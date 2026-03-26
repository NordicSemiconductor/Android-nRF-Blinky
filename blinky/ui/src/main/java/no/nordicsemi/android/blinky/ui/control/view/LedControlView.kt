package no.nordicsemi.android.blinky.ui.control.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.blinky.ui.R
import no.nordicsemi.android.blinky.ui.control.viewmodel.BlinkyViewModel

@Composable
internal fun LedControlView(
    state: Boolean,
    enabled: Boolean,
    onStateChanged: (Boolean) -> Unit,
    onBlink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .clickable(enabled = enabled) { onStateChanged(!state) }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = stringResource(R.string.blinky_led),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.blinky_led_descr),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = state,
                    onCheckedChange = onStateChanged,
                    enabled = enabled,
                )
            }

            // Blink action.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.blinky_led_blink_descr, BlinkyViewModel.BLINK_COUNT),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onBlink,
                    enabled = enabled,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LightMode,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp).size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.blinky_led_blink).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun LecControlViewPreview() {
    LedControlView(
        state = true,
        enabled = true,
        onStateChanged = {},
        onBlink = {},
        modifier = Modifier.padding(16.dp),
    )
}