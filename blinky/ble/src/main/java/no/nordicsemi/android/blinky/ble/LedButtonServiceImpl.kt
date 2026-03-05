package no.nordicsemi.android.blinky.ble

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.spec.BlinkySpec
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import kotlin.uuid.ExperimentalUuidApi

/**
 * Implementation of the [Blinky.State] interface for the LED Button Service (LBS).
 *
 * @param ledButtonService The service to use.
 * @param scope The connection scope. This scope gets canceled when the connection is closed.
 * @throws IllegalArgumentException If the service UUID is not [BlinkySpec.SERVICE_UUID],
 * or does not have required characteristics.
 */
@OptIn(ExperimentalUuidApi::class)
internal class LedButtonServiceImpl(
    ledButtonService: RemoteService,
    scope: CoroutineScope,
): Blinky.State {
    init {
        require(ledButtonService.uuid == BlinkySpec.SERVICE_UUID) {
            "Unrecognized service UUID: ${ledButtonService.uuid}"
        }
    }

    /**
     * The GATT characteristics of the LED Button Service (LBS) for controlling the LED state on
     * the remote peripheral.
     *
     * Possible values are:
     * * 0x00 - LED is off.
     * * 0x01 - LED is on.
     * @see BlinkySpec.LED_CHARACTERISTIC_UUID
     */
    private var ledCharacteristic: RemoteCharacteristic = ledButtonService.characteristics
        .first { it.uuid == BlinkySpec.LED_CHARACTERISTIC_UUID }

    /**
     * The GATT characteristics of the LED Button Service (LBS) notified when the Button state on
     * the remote peripheral changes.
     *
     * Possible values are:
     * * 0x00 - Button is released.
     * * 0x01 - Button is pressed.
     * @see BlinkySpec.BUTTON_CHARACTERISTIC_UUID
     */
    private var buttonCharacteristic: RemoteCharacteristic = ledButtonService.characteristics
        .first { it.uuid == BlinkySpec.BUTTON_CHARACTERISTIC_UUID }

    init {
        require(ledCharacteristic.isWritable()) {
            "LED characteristic must have WRITE or WRITE WITHOUT RESPONSE property"
        }
        require(buttonCharacteristic.isSubscribable()) {
            "Button characteristic must have NOTIFY or INDICATE property"
        }
    }

    override val led = MutableStateFlow(false)
        .also {
            scope.launch(Dispatchers.IO) {
                // Read initial state from the characteristic.
                val rawLedValue = ledCharacteristic.read()
                val ledValue = rawLedValue.state

                // Update the local state.
                it.update { ledValue }

                // Whenever the value changes, write the value to the characteristic.
                it.collect { value ->
                    val command = byteArrayOf(if (value) 1 else 0)
                    ledCharacteristic.write(command)
                }
            }
        }

    override val button = buttonCharacteristic.subscribe()
        .map { rawButtonValue -> rawButtonValue.state }
        .stateIn(scope, SharingStarted.Lazily, false)

    /**
     * Parses the raw value of LED and Button (0x00 or 0x01) to [Boolean].
     *
     * Note: In LED Button Service both LED and Button use the same state encoding.
     */
    private val ByteArray.state: Boolean
        get() = size == 1 && this[0] == 0x01.toByte()
}