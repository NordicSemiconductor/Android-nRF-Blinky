package no.nordicsemi.android.blinky

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import dagger.hilt.android.AndroidEntryPoint
import no.nordicsemi.android.blinky.navigation.BlinkyDestinations
import no.nordicsemi.android.blinky.navigation.ScannerDestinations
import no.nordicsemi.android.common.navigation.NavigationView
import no.nordicsemi.android.common.theme.NordicActivity
import no.nordicsemi.android.common.theme.NordicTheme

@AndroidEntryPoint
class MainActivity: NordicActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NordicTheme {
                Surface {
                    NavigationView(ScannerDestinations + BlinkyDestinations)
                }
            }
        }
    }
}