package no.nordicsemi.android.blinky.control.state

sealed class ConnectionState {

    object Connecting: ConnectionState()

    object Initializing: ConnectionState()

    object Connected: ConnectionState()

    data class Disconnected(val reason: String): ConnectionState()

}