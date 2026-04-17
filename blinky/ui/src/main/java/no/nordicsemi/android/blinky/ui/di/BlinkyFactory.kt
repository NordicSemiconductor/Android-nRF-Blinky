package no.nordicsemi.android.blinky.ui.di

import no.nordicsemi.android.blinky.spec.Blinky

/**
 * Factory for creating [Blinky] instances.
 *
 * This interface is used to separate the Blinky implementation from the UI.
 *
 * @see Blinky
 */
interface BlinkyFactory {
    /**
     * Creates a new instance of [Blinky].
     *
     * @param identifier The identifier of the device to connect to.
     */
    fun create(identifier: String): Blinky
}