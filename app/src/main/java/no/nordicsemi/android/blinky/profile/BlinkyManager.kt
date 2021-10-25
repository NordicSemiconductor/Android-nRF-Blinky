/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package no.nordicsemi.android.blinky.profile

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.livedata.ObservableBleManager
import no.nordicsemi.android.blinky.BuildConfig
import no.nordicsemi.android.blinky.profile.callback.BlinkyButtonDataCallback
import no.nordicsemi.android.blinky.profile.callback.BlinkyLedDataCallback
import no.nordicsemi.android.blinky.profile.data.BlinkyLED.turn
import no.nordicsemi.android.log.LogContract
import no.nordicsemi.android.log.LogSession
import no.nordicsemi.android.log.Logger
import java.util.*

class BlinkyManager(context: Context) : ObservableBleManager(context) {

    private val ledState = MutableLiveData<Boolean>()
    private val buttonState = MutableLiveData<Boolean>()
    private var buttonCharacteristic: BluetoothGattCharacteristic? = null
    private var ledCharacteristic: BluetoothGattCharacteristic? = null
    private var logSession: LogSession? = null
    private var supported = false
    private var ledOn = false

    fun getLedState(): LiveData<Boolean> {
        return ledState
    }

    fun getButtonState(): LiveData<Boolean> {
        return buttonState
    }

    override fun getGattCallback(): BleManagerGattCallback {
        return BlinkyBleManagerGattCallback()
    }

    /**
     * Sets the log session to be used for low level logging.
     * @param session the session, or null, if nRF Logger is not installed.
     */
    fun setLogger(session: LogSession?) {
        logSession = session
    }

    override fun log(priority: Int, message: String) {
        if (BuildConfig.DEBUG) {
            Log.println(priority, "BlinkyManager", message)
        }
        // The priority is a Log.X constant, while the Logger accepts it's log levels.
        Logger.log(logSession, LogContract.Log.Level.fromPriority(priority), message)
    }

    override fun shouldClearCacheWhenDisconnected(): Boolean {
        return !supported
    }

    /**
     * The Button callback will be notified when a notification from Button characteristic
     * has been received, or its data was read.
     *
     *
     * If the data received are valid (single byte equal to 0x00 or 0x01), the
     * [BlinkyButtonDataCallback.onButtonStateChanged] will be called.
     * Otherwise, the [BlinkyButtonDataCallback.onInvalidDataReceived]
     * will be called with the data received.
     */
    private val buttonCallback: BlinkyButtonDataCallback = object : BlinkyButtonDataCallback() {

        override fun onButtonStateChanged(device: BluetoothDevice, pressed: Boolean) {
            log(
                LogContract.Log.Level.APPLICATION,
                "Button " + if (pressed) "pressed" else "released"
            )
            buttonState.value = pressed
        }

        override fun onInvalidDataReceived(device: BluetoothDevice, data: Data) {
            log(Log.WARN, "Invalid data received: $data")
        }
    }

    /**
     * The LED callback will be notified when the LED state was read or sent to the target device.
     *
     *
     * This callback implements both [no.nordicsemi.android.ble.callback.DataReceivedCallback]
     * and [no.nordicsemi.android.ble.callback.DataSentCallback] and calls the same
     * method on success.
     *
     *
     * If the data received were invalid, the
     * [BlinkyLedDataCallback.onInvalidDataReceived] will be
     * called.
     */
    private val ledCallback: BlinkyLedDataCallback = object : BlinkyLedDataCallback() {
        override fun onLedStateChanged(
            device: BluetoothDevice,
            on: Boolean
        ) {
            ledOn = on
            log(LogContract.Log.Level.APPLICATION, "LED " + if (on) "ON" else "OFF")
            ledState.value = on
        }

        override fun onInvalidDataReceived(
            device: BluetoothDevice,
            data: Data
        ) {
            // Data can only invalid if we read them. We assume the app always sends correct data.
            log(Log.WARN, "Invalid data received: $data")
        }
    }

    /**
     * BluetoothGatt callbacks object.
     */
    private inner class BlinkyBleManagerGattCallback : BleManagerGattCallback() {
        override fun initialize() {
            setNotificationCallback(buttonCharacteristic).with(buttonCallback)
            readCharacteristic(ledCharacteristic).with(ledCallback).enqueue()
            readCharacteristic(buttonCharacteristic).with(buttonCallback).enqueue()
            enableNotifications(buttonCharacteristic).enqueue()
        }

        public override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service = gatt.getService(LBS_UUID_SERVICE)
            if (service != null) {
                buttonCharacteristic = service.getCharacteristic(LBS_UUID_BUTTON_CHAR)
                ledCharacteristic = service.getCharacteristic(LBS_UUID_LED_CHAR)
            }
            var writeRequest = false
            if (ledCharacteristic != null) {
                val ledProperties = ledCharacteristic!!.properties
                writeRequest = ledProperties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0
            }
            supported = buttonCharacteristic != null && ledCharacteristic != null && writeRequest
            return supported
        }

        override fun onServicesInvalidated() {
            buttonCharacteristic = null
            ledCharacteristic = null
        }
    }

    /**
     * Sends a request to the device to turn the LED on or off.
     *
     * @param on true to turn the LED on, false to turn it off.
     */
    fun turnLed(on: Boolean) {
        // Are we connected?
        if (ledCharacteristic == null) return

        // No need to change?
        if (ledOn == on) return
        log(Log.VERBOSE, "Turning LED " + (if (on) "ON" else "OFF") + "...")
        writeCharacteristic(
            ledCharacteristic,
            turn(on),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).with(ledCallback).enqueue()
    }

    companion object {
        /** Nordic Blinky Service UUID.  */
		@JvmField
		val LBS_UUID_SERVICE = UUID.fromString("00001523-1212-efde-1523-785feabcd123")

        /** BUTTON characteristic UUID.  */
        private val LBS_UUID_BUTTON_CHAR = UUID.fromString("00001524-1212-efde-1523-785feabcd123")

        /** LED characteristic UUID.  */
        private val LBS_UUID_LED_CHAR = UUID.fromString("00001525-1212-efde-1523-785feabcd123")
    }
}