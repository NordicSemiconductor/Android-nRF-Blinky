package no.nordicsemi.android.blinky.spec.exception

import no.nordicsemi.android.blinky.spec.Blinky

/**
 * Set of possible errors that can be thrown by a [Blinky] implementation.
 */
sealed class BlinkyException: IllegalStateException() {
    /** Connection to the peripheral failed. */
    class ConnectionFailed : BlinkyException()
    /** The connection to the peripheral timed out. */
    class Timeout : BlinkyException()
    /** The peripheral does not support the required LED Button Service (LBS). */
    class NotSupported : BlinkyException()
    /** The connection to the peripheral was lost. */
    class LinkLoss : BlinkyException()
}