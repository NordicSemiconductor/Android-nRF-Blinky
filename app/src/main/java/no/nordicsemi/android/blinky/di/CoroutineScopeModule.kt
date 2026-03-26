package no.nordicsemi.android.blinky.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class CoroutineScopeModule {

    @Provides
    @Singleton
    fun applicationScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}