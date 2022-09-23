package no.nordicsemi.android.blinky.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import no.nordicsemi.android.blinky.control.BlinkyDestination
import no.nordicsemi.android.blinky.control.BlinkyParams
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.transport_ble.BlinkyManager
import no.nordicsemi.android.common.navigation.NavigationManager

@Module
@InstallIn(ViewModelComponent::class)
abstract class BlinkyModule {

    companion object {

        @Provides
        fun provideBlinkyManager(
            @ApplicationContext context: Context,
            navigationManager: NavigationManager,
        ): BlinkyManager {
            val parameters = navigationManager.getArgument(BlinkyDestination) as BlinkyParams
            return BlinkyManager(context, parameters.device)
        }

    }

    @Binds
    abstract fun bindBlinky(
        BlinkManager: BlinkyManager
    ): Blinky

}