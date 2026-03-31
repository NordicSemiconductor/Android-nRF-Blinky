package no.nordicsemi.android.blinky

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import no.nordicsemi.android.blinky.ui.control.BlinkyDevice
import no.nordicsemi.android.blinky.ui.control.BlinkyKey
import no.nordicsemi.android.blinky.ui.control.blinkyEntry
import no.nordicsemi.android.common.theme.NordicActivity
import no.nordicsemi.android.common.theme.NordicTheme
import no.nordicsemi.android.scanner.ScannerKey
import no.nordicsemi.android.scanner.scannerEntry

@AndroidEntryPoint
class MainActivity: NordicActivity() {

    companion object {
        private const val EXTRA_DEVICE = "no.nordicsemi.android.blinky.EXTRA_DEVICE"
        private const val EXTRA_NAME = "no.nordicsemi.android.blinky.EXTRA_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the Activity was opened with an EXTRA_DEVICE in the Intent,
        // it should immediately launch the Blinky entry.
        val identifier = intent.getStringExtra(EXTRA_DEVICE)
        val name = intent.getStringExtra(EXTRA_NAME)
        intent.removeExtra(EXTRA_DEVICE)
        intent.removeExtra(EXTRA_NAME)

        setContent {
            NordicTheme {
                App(identifier, name)
            }
        }
    }
}

@Composable
private fun App(identifier: String?, name: String?) {
    val backStack = rememberNavBackStack(ScannerKey)
    if (identifier != null) {
        backStack.add(BlinkyKey(BlinkyDevice(identifier, name)))
    }

    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            scannerEntry(
                onDeviceSelected = { identifier, name ->
                    backStack.add(BlinkyKey(BlinkyDevice(identifier, name)))
                }
            )
            blinkyEntry(
                onNavigateUp = {
                    backStack.removeLastOrNull()
                }
            )
        }
    )
}