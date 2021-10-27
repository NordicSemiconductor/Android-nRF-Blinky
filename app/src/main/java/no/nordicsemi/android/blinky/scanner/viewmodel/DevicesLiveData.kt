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

import android.os.ParcelUuid
import androidx.lifecycle.LiveData
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice
import no.nordicsemi.android.blinky.profile.BlinkyManager
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.util.*

/**
 * This class keeps the current list of discovered Bluetooth LE devices matching filter.
 * Each time @{link [.applyFilter] is called, the observers are notified with a new
 * list instance.
 */
class DevicesLiveData  /* package */ internal constructor(
    private var filterUuidRequired: Boolean,
    private var filterNearbyOnly: Boolean
) : LiveData<List<DiscoveredBluetoothDevice>?>() {

    private val devices: MutableList<DiscoveredBluetoothDevice> = ArrayList()
    private var filteredDevices: List<DiscoveredBluetoothDevice>? = null

    /* package */
    fun bluetoothDisabled() {
        devices.clear()
        filteredDevices = null
        postValue(null)
    }

    /* package */
    fun filterByUuid(uuidRequired: Boolean): Boolean {
        filterUuidRequired = uuidRequired
        return applyFilter()
    }

    /* package */
    fun filterByDistance(nearbyOnly: Boolean): Boolean {
        filterNearbyOnly = nearbyOnly
        return applyFilter()
    }

    /* package */
    fun deviceDiscovered(result: ScanResult): Boolean {
        var device: DiscoveredBluetoothDevice

        // Check if it's a new device.
        val index = indexOf(result)
        if (index == -1) {
            device = DiscoveredBluetoothDevice(result)
            devices.add(device)
        } else {
            device = devices[index]
        }

        // Update RSSI and name.
        device = device.update(result)

        // Return true if the device was on the filtered list or is to be added.
        return (filteredDevices != null && filteredDevices!!.contains(device)
                || matchesUuidFilter(result) && matchesNearbyFilter(device.highestRssi))
    }

    /**
     * Clears the list of devices.
     */
    fun clear() {
        devices.clear()
        filteredDevices = null
        postValue(null)
    }

    /**
     * Refreshes the filtered device list based on the filter flags.
     */
    /* package */
    fun applyFilter(): Boolean {
        val tmp: MutableList<DiscoveredBluetoothDevice> = ArrayList()
        for (device in devices) {
            val result = device.scanResult
            if (matchesUuidFilter(result) && matchesNearbyFilter(device.highestRssi)) {
                tmp.add(device)
            }
        }
        filteredDevices = tmp
        postValue(filteredDevices)
        return filteredDevices?.isNotEmpty() == true
    }

    /**
     * Finds the index of existing devices on the device list.
     *
     * @param result scan result.
     * @return Index of -1 if not found.
     */
    private fun indexOf(result: ScanResult): Int {
        for ((i, device) in devices.withIndex()) {
            if (device.matches(result)) return i
        }
        return -1
    }

    private fun matchesUuidFilter(result: ScanResult): Boolean {
        if (!filterUuidRequired) return true
        val record = result.scanRecord ?: return false
        val uuids = record.serviceUuids ?: return false
        return uuids.contains(FILTER_UUID)
    }

    private fun matchesNearbyFilter(rssi: Int): Boolean {
        return if (!filterNearbyOnly) true else rssi >= FILTER_RSSI
    }

    companion object {
        private val FILTER_UUID = ParcelUuid(BlinkyManager.LBS_UUID_SERVICE)
        private const val FILTER_RSSI = -50 // [dBm]
    }
}