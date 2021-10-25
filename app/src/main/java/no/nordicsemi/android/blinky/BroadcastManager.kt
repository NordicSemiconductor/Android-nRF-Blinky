package no.nordicsemi.android.blinky

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter

class BroadcastManager(private val context: Context) {

    fun registerReceiver(receiver: BroadcastReceiver, intentFilter: IntentFilter) {
        context.registerReceiver(receiver, intentFilter)
    }

    fun unregisterReceiver(receiver: BroadcastReceiver) {
        context.unregisterReceiver(receiver)
    }
}
