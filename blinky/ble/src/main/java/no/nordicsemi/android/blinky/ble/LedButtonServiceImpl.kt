package no.nordicsemi.android.blinky.ble

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.spec.BlinkySpec
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
internal class LedButtonServiceImpl(
    ledButtonService: RemoteService,
    scope: CoroutineScope,
): Blinky.State {
    init {
        require(ledButtonService.uuid == BlinkySpec.BLINKY_SERVICE_UUID) {
            "Unrecognized service UUID: ${ledButtonService.uuid}"
        }
    }

    private var ledCharacteristic: RemoteCharacteristic = ledButtonService.characteristics
        .first { it.uuid == BlinkySpec.BLINKY_LED_CHARACTERISTIC_UUID }
    private var buttonCharacteristic: RemoteCharacteristic = ledButtonService.characteristics
        .first { it.uuid == BlinkySpec.BLINKY_BUTTON_CHARACTERISTIC_UUID }

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
                val ledValue = rawLedValue.size == 1 && rawLedValue.first() == 0x01.toByte()
                it.update { ledValue }

                // Whenever the value changes, write the value to the characteristic.
                try {
                    println("AAA 2.1. LED collection started")
                    it.collect { value ->
                        val command = byteArrayOf(if (value) 1 else 0)
                        ledCharacteristic.write(command)
                    }
                } catch (e: Exception) {
                    println("AAA 2.1. LED collection completed with catch ${e.message}")
                    throw e
                }
            }
        }

    override val button = buttonCharacteristic
        .subscribe()
        .map { bytes -> bytes.size == 1 && bytes[0] == 0x01.toByte() }
        .onStart {
            println("AAA 2.2. Subscription started")
        }
        .onCompletion {
            println("AAA 2.2. Subscription cancelled with: $it")
        }
        .stateIn(scope, SharingStarted.Lazily, false)
}