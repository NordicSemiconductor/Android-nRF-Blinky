package no.nordicsemi.android.blinky.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import jakarta.inject.Inject
import no.nordicsemi.android.blinky.ble.BlinkyManager
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.ui.di.BlinkyFactory
import no.nordicsemi.kotlin.ble.client.android.CentralManager

@Module
@InstallIn(ServiceComponent::class)
abstract class BlinkyModule {

    class BlinkyFactoryImpl @Inject constructor(
        private val centralManager: CentralManager,
    ): BlinkyFactory {

        override fun create(identifier: String): Blinky {
            val peripheral = centralManager.getPeripheralById(identifier)!!
            return BlinkyManager(centralManager, peripheral)
        }
    }

    @Binds
    @ServiceScoped
    abstract fun bind(
        impl: BlinkyFactoryImpl
    ): BlinkyFactory
}