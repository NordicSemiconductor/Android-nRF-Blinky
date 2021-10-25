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
package no.nordicsemi.android.blinky.adapter

import android.bluetooth.BluetoothDevice
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import no.nordicsemi.android.support.v18.scanner.ScanResult

@Parcelize
data class DiscoveredBluetoothDevice(
    val device: BluetoothDevice,
    val scanResult: ScanResult,
    val name: String? = null,
    val lastScanResult: ScanResult? = null,
    val rssi: Int = 0,
    val previousRssi: Int = 0,
    val highestRssi: Int = 0,
    val address: String = "",
) : Parcelable {

    constructor(result: ScanResult) : this(
        device = result.device,
        scanResult = result,
        name = if (result.scanRecord != null) result.scanRecord!!.deviceName else null,
        previousRssi = result.rssi,
        rssi = result.rssi,
        highestRssi = result.rssi
    )

    /**
     * This method returns true if the RSSI range has changed. The RSSI range depends on drawable
     * levels from [no.nordicsemi.android.blinky.R.drawable.ic_signal_bar].
     *
     * @return True, if the RSSI range has changed.
     */
    /* package */
    fun hasRssiLevelChanged(): Boolean {
        val newLevel =
            if (rssi <= 10) 0 else if (rssi <= 28) 1 else if (rssi <= 45) 2 else if (rssi <= 65) 3 else 4
        val oldLevel =
            if (previousRssi <= 10) 0 else if (previousRssi <= 28) 1 else if (previousRssi <= 45) 2 else if (previousRssi <= 65) 3 else 4
        return newLevel != oldLevel
    }

    /**
     * Updates the device values based on the scan result.
     *
     * @param scanResult the new received scan result.
     */
    fun update(scanResult: ScanResult): DiscoveredBluetoothDevice {
        return copy(
            lastScanResult = scanResult,
            name = scanResult.scanRecord?.deviceName,
            previousRssi = rssi,
            rssi = scanResult.rssi,
            highestRssi = if (highestRssi > rssi) highestRssi else rssi
        )
    }

    fun matches(scanResult: ScanResult): Boolean {
        return device.address == scanResult.device.address
    }

    override fun hashCode(): Int {
        return device.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is DiscoveredBluetoothDevice) {
            return device.address == other.device.address
        }
        return super.equals(other)
    }
}
