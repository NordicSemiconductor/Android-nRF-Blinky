package no.nordicsemi.android.blinky.spec

import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class BlinkySpec {

    companion object {
        /** If a button is pressed for more than this value it is reported as long press. */
        val LONG_PRESS_TIMEOUT = 1.seconds
        /** The LED Button Service UUID. */
        val SERVICE_UUID: Uuid = Uuid.parse("00001523-1212-efde-1523-785feabcd123")
        /** The UUID of the Button characteristic. */
        val BUTTON_CHARACTERISTIC_UUID: Uuid = Uuid.parse("00001524-1212-efde-1523-785feabcd123")
        /** The UUID of the LED characteristic. */
        val LED_CHARACTERISTIC_UUID: Uuid = Uuid.parse("00001525-1212-efde-1523-785feabcd123")
    }

}