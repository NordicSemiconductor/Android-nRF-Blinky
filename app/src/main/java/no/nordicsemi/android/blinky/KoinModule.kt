package no.nordicsemi.android.blinky

import no.nordicsemi.android.blinky.utils.Utils
import no.nordicsemi.android.blinky.scanner.viewmodel.ScannerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val koinModule = module {
    viewModel { ScannerViewModel(get(), get(), get()) }

    single { Utils(get(), get()) }
    single { LocalDataProvider(get()) }
}
