package no.nordicsemi.android.blinky.spec

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class BlinkySpec {

    companion object {
        val BLINKY_SERVICE_UUID: Uuid = Uuid.parse("00001523-1212-efde-1523-785feabcd123")
        val BLINKY_BUTTON_CHARACTERISTIC_UUID: Uuid = Uuid.parse("00001524-1212-efde-1523-785feabcd123")
        val BLINKY_LED_CHARACTERISTIC_UUID: Uuid = Uuid.parse("00001525-1212-efde-1523-785feabcd123")
    }

}