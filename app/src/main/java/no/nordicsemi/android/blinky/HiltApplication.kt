package no.nordicsemi.android.blinky

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import no.nordicsemi.android.blinky.di.BluetoothLifecycleObserver
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class HiltApplication : Application() {

    @Inject
    internal lateinit var bluetoothLifecycleObserver: BluetoothLifecycleObserver

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber. By default, the library will log to the Android logcat.
        Timber.plant(Timber.DebugTree())
        
        // Register the observer to manage Bluetooth environment lifecycle.
        registerActivityLifecycleCallbacks(bluetoothLifecycleObserver)
    }
}