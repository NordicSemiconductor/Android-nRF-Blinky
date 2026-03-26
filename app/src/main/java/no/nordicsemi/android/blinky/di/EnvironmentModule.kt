package no.nordicsemi.android.blinky.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import no.nordicsemi.kotlin.ble.core.android.AndroidEnvironment
import no.nordicsemi.kotlin.ble.environment.android.NativeAndroidEnvironment
import javax.inject.Singleton

/**
 * This module provides the Environment in which the app is running,
 * and makes sure it gets closed when no longer needed.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EnvironmentModule {

    companion object {

        @Provides
        @Singleton
        fun provideEnvironment(
            @ApplicationContext context: Context,
        ): NativeAndroidEnvironment = NativeAndroidEnvironment.getInstance(context, isNeverForLocationFlagSet = true)
    }

    @Binds
    abstract fun bindEnvironment(environment: NativeAndroidEnvironment): AndroidEnvironment
}