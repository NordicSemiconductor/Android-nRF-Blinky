package no.nordicsemi.android.blinky.spec.exception

import no.nordicsemi.android.blinky.spec.Blinky

/**
 * Set of possible errors that can be thrown by a [Blinky] implementation.
 */
sealed class BlinkyException(message: String): IllegalStateException(message) {
    /** Connection to the peripheral failed. */
    class ConnectionFailed : BlinkyException("Connection failed")
    /** The connection to the peripheral timed out. */
    class Timeout : BlinkyException("Connection timed out")
    /** The peripheral does not support the required LED Button Service (LBS). */
    class NotSupported : BlinkyException("Device is not supported")
    /** The connection to the peripheral was lost. */
    class LinkLoss : BlinkyException("Connection lost")
}