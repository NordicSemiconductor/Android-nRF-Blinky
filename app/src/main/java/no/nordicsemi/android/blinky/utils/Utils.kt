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
package no.nordicsemi.android.blinky.utils

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import no.nordicsemi.android.blinky.LocalDataProvider

class Utils(
    private val context: Context,
    private val dataProvider: LocalDataProvider
) {

    /**
     * Checks whether Bluetooth is enabled.
     *
     * @return true if Bluetooth is enabled, false otherwise.
     */
    val isBleEnabled: Boolean
        get() {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            return adapter != null && adapter.isEnabled
        }

    /**
     * Returns whether Bluetooth Scan permission has been granted.
     *
     * @param context the context.
     * @return Whether Bluetooth Scan permission has been granted.
     */
    fun isBluetoothScanPermissionGranted(): Boolean {
        return if (!dataProvider.isSorAbove) true else ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns whether Bluetooth Connect permission has been granted.
     *
     * @param context the context.
     * @return Whether Bluetooth Connect permission has been granted.
     */
    fun isBluetoothConnectPermissionGranted(): Boolean {
        return if (!dataProvider.isSorAbove) true else ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }// Location is required only for Android 6-11.

    /**
     * Returns whether location permission and service is required in order to scan
     * for Bluetooth LE devices. This app does not need beacons and other location-intended
     * devices, and requests BLUETOOTH_SCAN permission with "never for location" flag.
     *
     * @return Whether the location permission and service running are required.
     */
    val isLocationPermissionRequired: Boolean
        get() =// Location is required only for Android 6-11.
            dataProvider.isMarshmallowOrAbove && !dataProvider.isSorAbove

    /**
     * Checks for required permissions.
     *
     * @return True if permissions are already granted, false otherwise.
     */
    fun isLocationPermissionGranted(): Boolean {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }

    /**
     * Returns true if Bluetooth Scan permission has been requested at least twice and
     * user denied it, and checked 'Don't ask again'.
     *
     * @param activity the activity.
     * @return True if permission has been denied and the popup will not come up any more,
     * false otherwise.
     */
    fun isBluetoothScanPermissionDeniedForever(activity: Activity): Boolean {
        return (!isLocationPermissionGranted() // Location permission must be denied
                && dataProvider.bluetoothPermissionRequested
                && !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )) // This method should return false
    }

    /**
     * Returns true if location permission has been requested at least twice and
     * user denied it, and checked 'Don't ask again'.
     *
     * @param activity the activity.
     * @return True if permission has been denied and the popup will not come up any more,
     * false otherwise.
     */
    fun isLocationPermissionDeniedForever(activity: Activity): Boolean {
        return (!isLocationPermissionGranted() // Location permission must be denied
                && dataProvider.locationPermissionRequested // Permission must have been requested before
                && !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
                ) // This method should return false
    }

    /**
     * On some devices running Android Marshmallow or newer location services must be enabled in
     * order to scan for Bluetooth LE devices. This method returns whether the Location has been
     * enabled or not.
     *
     * @return True on Android 6.0+ if location mode is different than LOCATION_MODE_OFF.
     * It always returns true on Android versions prior to Marshmallow.
     */
    fun isLocationEnabled(): Boolean {
        if (dataProvider.isMarshmallowOrAbove) {
            val lm = context.getSystemService(LocationManager::class.java)
            return LocationManagerCompat.isLocationEnabled(lm)
        }
        return true
    }
}