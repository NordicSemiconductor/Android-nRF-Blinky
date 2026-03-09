package no.nordicsemi.android.blinky

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import no.nordicsemi.android.blinky.ui.control.BlinkyKey
import no.nordicsemi.android.blinky.ui.control.blinkyEntry
import no.nordicsemi.android.blinky.ui.scanner.ScannerKey
import no.nordicsemi.android.blinky.ui.scanner.scannerEntry
import no.nordicsemi.android.common.theme.NordicActivity
import no.nordicsemi.android.common.theme.NordicTheme

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
    val backStack = remember { mutableStateListOf<NavKey>(ScannerKey) }

    NavDisplay(
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            scannerEntry(
                onDeviceSelected = { device ->
                    backStack.add(BlinkyKey(device))
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