package no.nordicsemi.android.blinky

import android.content.Context
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice
import no.nordicsemi.android.log.LogSession
import no.nordicsemi.android.log.Logger

class LoggerProvider(private val context: Context) {

    fun createNewSession(target: DiscoveredBluetoothDevice): LogSession? {
        return Logger.newSession(context, null, target.address, target.name)
    }
}
