package no.nordicsemi.android.blinky.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.native
import no.nordicsemi.kotlin.ble.core.android.AndroidEnvironment
import no.nordicsemi.kotlin.ble.environment.android.NativeAndroidEnvironment
import javax.inject.Singleton

/**
 * This module provides the Central Manager used to scan and connect to Bluetooth LE peripherals.
 */
@Module
@InstallIn(SingletonComponent::class)
internal class CentralManagerModule {

    @Provides
    @Singleton
    internal fun provideCentralManagerBuilder(): CentralManagerBuilder = object : CentralManagerBuilder {
        override fun create(
            environment: AndroidEnvironment,
            scope: CoroutineScope
        ): CentralManager = CentralManager.native(environment = environment as NativeAndroidEnvironment, scope = scope)
    }

    @Provides
    // Not a Singleton!
    internal fun provideCentralManager(
        manager: BluetoothLifecycleManager,
    ) = manager.centralManager
}