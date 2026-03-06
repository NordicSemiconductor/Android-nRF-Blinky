package no.nordicsemi.android.blinky.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import no.nordicsemi.kotlin.ble.core.android.AndroidEnvironment
import no.nordicsemi.kotlin.ble.environment.android.NativeAndroidEnvironment

/**
 * This module provides the Environment in which the app is running,
 * and makes sure it gets closed when no longer needed.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class EnvironmentModule {

    companion object {

        @Provides
        @ActivityRetainedScoped
        fun provideEnvironment(
            @ApplicationContext context: Context,
            lifecycle: ActivityRetainedLifecycle,
        ): NativeAndroidEnvironment {
            // Make sure the environment is closed when the lifecycle is cleared.
            // This will unregister the broadcast receiver.
            return NativeAndroidEnvironment.getInstance(context, isNeverForLocationFlagSet = true)
                .also {
                    lifecycle.addOnClearedListener { it.close() }
                }
        }
    }

    @Binds
    abstract fun bindEnvironment(environment: NativeAndroidEnvironment): AndroidEnvironment
}