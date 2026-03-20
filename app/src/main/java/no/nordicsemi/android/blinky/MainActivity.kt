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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NordicTheme {
                App()
            }
        }
    }
}

@Composable
private fun App() {
    val backStack = rememberNavBackStack(ScannerKey)

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