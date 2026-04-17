package no.nordicsemi.android.blinky.ui.control.util

import kotlinx.coroutines.delay
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.spec.toggle

/**
 * Blinks the LED on the device.
 *
 * @param count The number of times to blink the LED.
 */
suspend fun Blinky.State.blink(count: Int) {
    repeat(count * 2) {
        led.toggle()
        delay(500)
    }
}