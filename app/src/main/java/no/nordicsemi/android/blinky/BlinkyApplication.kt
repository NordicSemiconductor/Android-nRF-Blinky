package no.nordicsemi.android.blinky

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BlinkyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Added to support vector drawables for devices below Android 21.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }

        startKoin {
            // declare used Android context
            androidContext(this@BlinkyApplication)
            // declare modules
            modules(koinModule)
        }
    }
}
