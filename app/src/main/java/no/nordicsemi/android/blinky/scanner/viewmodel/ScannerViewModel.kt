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
package no.nordicsemi.android.blinky.scanner.viewmodel

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import no.nordicsemi.android.blinky.BroadcastManager
import no.nordicsemi.android.blinky.LocalDataProvider
import no.nordicsemi.android.blinky.utils.Utils
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings

class ScannerViewModel(
    private val utils: Utils,
    private val dataProvider: LocalDataProvider,
    private val broadcastManager: BroadcastManager
) : ViewModel() {
    /**
     * MutableLiveData containing the list of devices.
     */
    val devices: DevicesLiveData = DevicesLiveData(dataProvider.uuidRequired, dataProvider.nearbyOnly)

    /**
     * MutableLiveData containing the scanner state.
     */
    val scannerState: ScannerStateLiveData = ScannerStateLiveData()

    init {
        registerBroadcastReceivers()
    }

    override fun onCleared() {
        super.onCleared()
        broadcastManager.unregisterReceiver(bluetoothStateBroadcastReceiver)
        if (dataProvider.isMarshmallowOrAbove) {
            broadcastManager.unregisterReceiver(locationProviderChangedReceiver)
        }
    }

    /**
     * Forces the observers to be notified. This method is used to refresh the screen after the
     * location permission has been granted. In result, the observer in
     * [no.nordicsemi.android.blinky.ScannerActivity] will try to start scanning.
     */
    fun refresh() {
        scannerState.refresh()
    }

    /**
     * Updates the device filter. Devices that once passed the filter will still be shown
     * even if they move away from the phone, or change the advertising packet. This is to
     * avoid removing devices from the list.
     *
     * @param uuidRequired if true, the list will display only devices with Led-Button Service UUID
     * in the advertising packet.
     */
    fun filterByUuid(uuidRequired: Boolean) {
        dataProvider.uuidRequired = uuidRequired
        if (devices.filterByUuid(uuidRequired)) scannerState.recordFound() else scannerState.clearRecords()
    }

    /**
     * Updates the device filter. Devices that once passed the filter will still be shown
     * even if they move away from the phone, or change the advertising packet. This is to
     * avoid removing devices from the list.
     *
     * @param nearbyOnly if true, the list will show only devices with high RSSI.
     */
    fun filterByDistance(nearbyOnly: Boolean) {
        dataProvider.nearbyOnly = nearbyOnly
        if (devices.filterByDistance(nearbyOnly)) scannerState.recordFound() else scannerState.clearRecords()
    }

    /**
     * Start scanning for Bluetooth devices.
     */
    fun startScan() {
        if (scannerState.isScanning()) {
            return
        }

        // Scanning settings
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(500)
            .setUseHardwareBatchingIfSupported(false)
            .build()
        val scanner = BluetoothLeScannerCompat.getScanner()
        scanner.startScan(null, settings, scanCallback)
        scannerState.scanningStarted()
    }

    /**
     * Stop scanning for bluetooth devices.
     */
    fun stopScan() {
        if (scannerState.isScanning() && scannerState.isBluetoothEnabled()) {
            val scanner = BluetoothLeScannerCompat.getScanner()
            scanner.stopScan(scanCallback)
            scannerState.scanningStopped()
        }
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // This callback will be called only if the scan report delay is not set or is set to 0.

            // If the packet has been obtained while Location was disabled, mark Location as not required
            if (utils.isLocationPermissionRequired && !utils.isLocationEnabled()) {
                dataProvider.isLocationPermissionRequired = false
            }
            if (devices.deviceDiscovered(result)) {
                devices.applyFilter()
                scannerState.recordFound()
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            // This callback will be called only if the report delay set above is greater then 0.

            // If the packet has been obtained while Location was disabled, mark Location as not required
            if (utils.isLocationPermissionRequired && !utils.isLocationEnabled()) {
                dataProvider.isLocationPermissionRequired = false
            }
            var atLeastOneMatchedFilter = false
            for (result in results) {
                atLeastOneMatchedFilter = devices.deviceDiscovered(result) || atLeastOneMatchedFilter
            }
            if (atLeastOneMatchedFilter) {
                devices.applyFilter()
                scannerState.recordFound()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            // TODO This should be handled
            scannerState.scanningStopped()
        }
    }

    /**
     * Register for required broadcast receivers.
     */
    private fun registerBroadcastReceivers() {
        broadcastManager.registerReceiver(
            bluetoothStateBroadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        if (dataProvider.isMarshmallowOrAbove) {
            broadcastManager.registerReceiver(
                locationProviderChangedReceiver,
                IntentFilter(LocationManager.MODE_CHANGED_ACTION)
            )
        }
    }

    /**
     * Broadcast receiver to monitor the changes in the location provider.
     */
    private val locationProviderChangedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val enabled = utils.isLocationEnabled()
            scannerState.setLocationEnabled(enabled)
        }
    }

    /**
     * Broadcast receiver to monitor the changes in the bluetooth adapter.
     */
    private val bluetoothStateBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
            val previousState = intent.getIntExtra(
                BluetoothAdapter.EXTRA_PREVIOUS_STATE,
                BluetoothAdapter.STATE_OFF
            )
            when (state) {
                BluetoothAdapter.STATE_ON -> scannerState.bluetoothEnabled()
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
                    stopScan()
                    scannerState.bluetoothDisabled()
                }
            }
        }
    }
}
