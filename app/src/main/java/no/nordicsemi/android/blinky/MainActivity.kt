package no.nordicsemi.android.blinky

import android.os.Bundle
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import no.nordicsemi.android.blinky.control.Blinky
import no.nordicsemi.android.blinky.control.BlinkyDestinations
import no.nordicsemi.android.blinky.scanner.Scanner
import no.nordicsemi.android.blinky.scanner.ScannerDestinations
import no.nordicsemi.android.common.navigation.NavigationView
import no.nordicsemi.android.common.theme.NordicActivity
import no.nordicsemi.android.common.theme.NordicTheme

@AndroidEntryPoint
class MainActivity: NordicActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NordicTheme {
                NavigationView(ScannerDestinations + BlinkyDestinations) { from, _ ->
                    when (from) {
                        Scanner -> Blinky
                        else -> null
                    }
                }
            }
        }
    }
}