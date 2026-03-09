package no.nordicsemi.android.blinky.ui.di

import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.ui.control.BlinkyDevice

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
     * @param device The device to connect to.
     */
    fun create(device: BlinkyDevice): Blinky
}