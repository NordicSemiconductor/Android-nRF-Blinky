package no.nordicsemi.android.blinky.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import jakarta.inject.Inject
import no.nordicsemi.android.blinky.ble.BlinkyManager
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.ui.control.BlinkyDevice
import no.nordicsemi.android.blinky.ui.di.BlinkyFactory
import no.nordicsemi.kotlin.ble.client.android.CentralManager

@Module
@InstallIn(ViewModelComponent::class)
abstract class BlinkyModule {

    class BlinkyFactoryImpl @Inject constructor(
        private val centralManager: CentralManager,
    ): BlinkyFactory {

        override fun create(device: BlinkyDevice): Blinky {
            val peripheral = centralManager.getPeripheralById(device.identifier)!!
            return BlinkyManager(centralManager, peripheral)
        }
    }

    @Binds
    @ViewModelScoped
    abstract fun bind(
        impl: BlinkyFactoryImpl
    ): BlinkyFactory
}