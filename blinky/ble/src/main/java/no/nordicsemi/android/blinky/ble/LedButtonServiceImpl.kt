package no.nordicsemi.android.blinky.ble

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.spec.BlinkySpec
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.client.exception.InvalidAttributeException
import no.nordicsemi.kotlin.ble.client.exception.OperationFailedException
import no.nordicsemi.kotlin.ble.core.exception.BluetoothException
import timber.log.Timber
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
                try {
                    // Read initial state from the characteristic.
                    val rawLedValue = ledCharacteristic.read()
                    val ledValue = rawLedValue.state

                    // Update the local state.
                    it.update { ledValue }
                } catch (e: OperationFailedException) {
                    // In some implementations the Button characteristic is not readable.
                    Timber.w("Reading LED characteristic failed: ${e.message}")
                }

                // Whenever the value changes, write the value to the characteristic.
                it.collect { value ->
                    try {
                        val command = byteArrayOf(if (value) 1 else 0)
                        ledCharacteristic.write(command)
                    } catch (_: InvalidAttributeException) {
                        // This exception is thrown when the device disconnects, or invalidates services.
                        Timber.w("Services invalidated before writing to LED characteristic")
                    } catch (e: OperationFailedException) {
                        // This exception is thrown when the device disconnects, but the client
                        // doesn't know about it before writing to the characteristic.
                        // This usually indicates error 133.
                        Timber.w("Writing to LED characteristic failed: ${e.message}")
                    } catch (e: BluetoothException) {
                        // Other errors.
                        Timber.e("Writing to LED characteristic failed: ${e.message}")
                    }
                }
            }
        }

    override val button: StateFlow<Boolean> = MutableStateFlow(false)
        .also { flow ->
            scope.launch {
                try {
                    // Read initial state from the characteristic.
                    val rawButtonValue = buttonCharacteristic.read()
                    val pressed = rawButtonValue.state

                    // Update the local state.
                    flow.update { pressed }
                } catch (e: OperationFailedException) {
                    // In some implementations the Button characteristic is not readable.
                    Timber.w("Reading button characteristic failed: ${e.message}")
                }

                // By having a local mutable flow we call subscribe() only once.
                buttonCharacteristic.subscribe()
                    .collect { flow.emit(it.state) }
            }
        }

    override val buttonPressed: Flow<Unit> = flow {
        /** Flag set when a long press is detected. */
        var isLongPress = false

        // Drop the initial value, button state is a StateFlow.
        button.drop(1).collect { pressed ->
            // If the button was pressed, start a timeout to detect a long press events.
            if (pressed) {
                try {
                    withTimeout(BlinkySpec.LONG_PRESS_TIMEOUT) {
                        // Await the button to be released before the timeout.
                        button.drop(1).filter { !it }.first()
                    }
                } catch (_: TimeoutCancellationException) {
                    // Button has not been released before the time runed out.
                    isLongPress = true
                }
            } else {
                // If released, emit the event only if it was not a long press.
                if (!isLongPress) {
                    emit(Unit)
                }
                isLongPress = false
            }
        }
    }

    override val buttonLongPressed: Flow<Unit> = button
        .flatMapLatest { pressed ->
            if (pressed) flow {
                delay(BlinkySpec.LONG_PRESS_TIMEOUT)
                emit(Unit)
            } else emptyFlow()
        }

    /**
     * Parses the raw value of LED and Button (0x00 or 0x01) to [Boolean].
     *
     * Note: In LED Button Service both LED and Button use the same state encoding.
     */
    private val ByteArray.state: Boolean
        get() = size == 1 && this[0] == 0x01.toByte()
}