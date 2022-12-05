package no.nordicsemi.android.blinky.control.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.log.LogContract
import no.nordicsemi.android.log.timber.nRFLoggerTree
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

/**
 *
 * @param context The application context.
 * @param deviceId The device ID.
 * @param deviceName The name of the Blinky device, as advertised.
 * @property blinky The Blinky implementation.
 */
class BlinkyRepository @Inject constructor(
    @ApplicationContext context: Context,
    @Named("deviceId") deviceId: String,
    @Named("deviceName") deviceName: String,
    private val blinky: Blinky,
): Blinky by blinky {
    /** Timber tree that logs to nRF Logger. */
    private val tree: Timber.Tree

    /** If the nRF Logger is installed, this URI will allow to open the session. */
    internal val sessionUri: Uri?

    init {
        // Plant a new Tree that logs to nRF Logger.
        tree = nRFLoggerTree(context, null, deviceId, deviceName)
            .also { Timber.plant(it) }
            .also { sessionUri = it.session?.sessionUri }
    }

    val loggedLedState: Flow<Boolean>
        get() = blinky.ledState.onEach {
            // Although Timber log levels are the same as LogCat's, nRF Logger has its own.
            // All standard log levels are mapped to the corresponding nRF Logger's levels:
            // https://github.com/NordicSemiconductor/nRF-Logger-API/blob/f90d5834c46cc2057b6a9f39dcbb8f2f2dd45d56/log-timber/src/main/java/no/nordicsemi/android/log/timber/nRFLoggerTree.java#L104
            // However, in order to log in nRF Logger on APPLICATION level, we need to use
            // that level explicitly.
            when(it) {
                true -> Timber.log(LogContract.Log.Level.APPLICATION, "LED turned ON")
                false -> Timber.log(LogContract.Log.Level.APPLICATION, "LED turned OFF")
            }
        }

    val loggedButtonState: Flow<Boolean>
        get() = blinky.buttonState.onEach {
            // The same applies here.
            when(it) {
                true -> Timber.log(LogContract.Log.Level.APPLICATION, "Button pressed")
                false -> Timber.log(LogContract.Log.Level.APPLICATION, "Button released")
            }
        }

    override fun release() {
        Timber.uproot(tree)
        blinky.release()
    }
}