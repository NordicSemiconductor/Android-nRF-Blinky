package no.nordicsemi.android.blinky.control.repository

import no.nordicsemi.android.blinky.spec.Blinky
import javax.inject.Inject

class BlinkyRepository @Inject constructor(
    val blinky: Blinky
): Blinky by blinky