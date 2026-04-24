package no.nordicsemi.android.blinky.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import no.nordicsemi.kotlin.ble.core.android.AndroidEnvironment
import no.nordicsemi.kotlin.ble.environment.android.mock.MockAndroidEnvironment
import javax.inject.Singleton

/**
 * This module provides the Environment in which the app is running.
 */
@Module
@InstallIn(SingletonComponent::class)
internal class EnvironmentModule {

    @Provides
    @Singleton
    internal fun provideEnvironmentBuilder(
    ): EnvironmentBuilder = object : EnvironmentBuilder {
        override fun create(): AndroidEnvironment = MockAndroidEnvironment.Api31(
            isBluetoothEnabled = true,
            isBluetoothConnectPermissionGranted = true,
            isBluetoothScanPermissionGranted = true,
            isNeverForLocationFlagSet = true,
        )
    }

    @Provides
    // Not a Singleton!
    internal fun provideEnvironment(
        manager: BluetoothLifecycleManager,
    ) = manager.environment

}