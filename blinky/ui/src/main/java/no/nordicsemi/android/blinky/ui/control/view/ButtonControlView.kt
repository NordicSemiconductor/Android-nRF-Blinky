package no.nordicsemi.android.blinky.ui.control.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import no.nordicsemi.android.blinky.spec.BlinkySpec
import no.nordicsemi.android.blinky.ui.R
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun ButtonControlView(
    state: Boolean,
    buttonPressed: Flow<Unit>,
    buttonLongPressed: Flow<Unit>,
    modifier: Modifier = Modifier,
) {
    var click by remember { mutableStateOf(false) }
    var longClick by remember { mutableStateOf(false) }

    LaunchedEffect(buttonPressed) {
        buttonPressed.collect {
            click = true
            delay(250.milliseconds)
            click = false
        }
    }
    LaunchedEffect(buttonLongPressed) {
        buttonLongPressed.collect {
            longClick = true
            delay(500.milliseconds)
            longClick = false
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (state) 1f else 0f,
        animationSpec = if (state)
            tween(BlinkySpec.LONG_PRESS_TIMEOUT.inWholeMilliseconds.toInt(), easing = LinearEasing)
        else spring()
    )

    OutlinedCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    imageVector = Icons.Default.RadioButtonChecked,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = stringResource(R.string.blinky_button),
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
                    text = stringResource(R.string.blinky_button_descr),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (state) stringResource(R.string.blinky_on) else stringResource(R.string.blinky_off),
                )
            }

            // Click indicator.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.blinky_button_press_descr),
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .background(
                            color = if (click)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .widthIn(min = 56.dp)
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp).size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.blinky_button_press).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            // Long click indicator.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.blinky_button_long_press_descr),
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .background(
                            color = if (longClick)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .widthIn(min = 56.dp)
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WatchLater,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp).size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.blinky_button_long_press).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { progress },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            // Legend.
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WatchLater,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp).size(12.dp)
                    )
                    Text(
                        text = stringResource(R.string.blinky_button_long_press_legend, BlinkySpec.LONG_PRESS_TIMEOUT.inWholeSeconds).uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun ButtonControlViewPreview() {
    ButtonControlView(
        state = true,
        buttonPressed = flowOf(Unit),
        buttonLongPressed = emptyFlow(),
        modifier = Modifier.padding(16.dp),
    )
}