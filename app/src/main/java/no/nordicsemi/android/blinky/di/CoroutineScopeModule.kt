package no.nordicsemi.android.blinky.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.components.ActivityRetainedComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@Module
@InstallIn(ActivityRetainedComponent::class)
class CoroutineScopeModule {

    @Provides
    fun applicationScope(
        lifecycle: ActivityRetainedLifecycle,
    ) = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        .also {
            // Cancel the scope when the Activity gets destroyed for good.
            lifecycle.addOnClearedListener { it.cancel() }
        }
}