package no.nordicsemi.android.blinky.di

import android.app.Service
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
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
internal abstract class BlinkyModule {

    internal class BlinkyFactoryImpl @Inject constructor(
        private val centralManager: CentralManager,
        service: Service,
        manager: BluetoothLifecycleManager,
    ): BlinkyFactory {

        // This initiator registers a Service lifecycle observer to notify the BluetoothLifecycleManager
        // when the Service is created and destroyed.
        //
        // This is possible, because the service is injecting the BlinkyFactory. Otherwise, the
        // manager should be informed manually in onCreate and onDestroy methods.
        init {
            val lifecycle = (service as? LifecycleOwner)?.lifecycle
            lifecycle?.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(
                    source: LifecycleOwner,
                    event: Lifecycle.Event
                ) = when (event) {
                    Lifecycle.Event.ON_CREATE -> manager.onComponentCreated()
                    Lifecycle.Event.ON_DESTROY -> manager.onComponentDestroyed()
                    else -> {}
                }
            })
        }

        override fun create(identifier: String): Blinky {
            val peripheral = centralManager.getPeripheralById(identifier)!!
            return BlinkyManager(centralManager, peripheral)
        }
    }

    @Binds
    @ServiceScoped
    internal abstract fun bind(
        impl: BlinkyFactoryImpl
    ): BlinkyFactory
}