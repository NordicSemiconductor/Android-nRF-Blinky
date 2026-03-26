package no.nordicsemi.android.blinky.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.native
import no.nordicsemi.kotlin.ble.environment.android.NativeAndroidEnvironment
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class CentralManagerModule {

    @Provides
    @Singleton
    fun provideCentralManager(
        environment: NativeAndroidEnvironment,
        scope: CoroutineScope,
    ) = CentralManager.native(environment, scope)
}