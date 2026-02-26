package no.nordicsemi.android.blinky.di

import androidx.lifecycle.SavedStateHandle
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import no.nordicsemi.android.blinky.ble.BlinkyManager
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.ui.control.Blinky
import no.nordicsemi.android.blinky.ui.control.BlinkyDevice
import no.nordicsemi.android.common.navigation.get
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.Peripheral

@Module
@InstallIn(ViewModelComponent::class)
abstract class BlinkyModule {

    companion object {

        @Provides
        @ViewModelScoped
        fun provideBlinkyDevice(handle: SavedStateHandle): BlinkyDevice {
            return handle.get(Blinky)
        }

        @Provides
        @ViewModelScoped
        fun providePeripheral(
            centralManager: CentralManager,
            device: BlinkyDevice,
        ) = centralManager.getPeripheralById(device.identifier)!!

        @Provides
        @ViewModelScoped
        fun provideBlinkyManager(
            centralManager: CentralManager,
            peripheral: Peripheral,
        ) = BlinkyManager(centralManager, peripheral)
    }

    @Binds
    abstract fun bindBlinky(
        blinkyManager: BlinkyManager
    ): Blinky

}