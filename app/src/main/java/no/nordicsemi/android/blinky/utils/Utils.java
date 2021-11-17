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

package no.nordicsemi.android.blinky.utils;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

public class Utils {
	private static final String PREFS_LOCATION_REQUIRED = "location_required";
	private static final String PREFS_PERMISSION_REQUESTED = "permission_requested";
	private static final String PREFS_BLUETOOTH_PERMISSION_REQUESTED = "bluetooth_permission_requested";

	/**
	 * Checks whether Bluetooth is enabled.
	 *
	 * @return true if Bluetooth is enabled, false otherwise.
	 */
	public static boolean isBleEnabled() {
		final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		return adapter != null && adapter.isEnabled();
	}

	/**
	 * Returns whether Bluetooth Scan permission has been granted.
	 *
	 * @param context the context.
	 * @return Whether Bluetooth Scan permission has been granted.
	 */
	public static boolean isBluetoothScanPermissionGranted(@NonNull final Context context) {
		if (!isSorAbove())
			return true;
		return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
				== PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * Returns whether Bluetooth Connect permission has been granted.
	 *
	 * @param context the context.
	 * @return Whether Bluetooth Connect permission has been granted.
	 */
	public static boolean isBluetoothConnectPermissionGranted(@NonNull final Context context) {
		if (!isSorAbove())
			return true;
		return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
				== PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * Returns whether location permission and service is required in order to scan
	 * for Bluetooth LE devices. This app does not need beacons and other location-intended
	 * devices, and requests BLUETOOTH_SCAN permission with "never for location" flag.
	 *
	 * @return Whether the location permission and service running are required.
	 */
	public static boolean isLocationPermissionRequired() {
		// Location is required only for Android 6-11.
		return isMarshmallowOrAbove() && !isSorAbove();
	}

	/**
	 * Checks for required permissions.
	 *
	 * @return True if permissions are already granted, false otherwise.
	 */
	public static boolean isLocationPermissionGranted(@NonNull final Context context) {
		return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * Returns true if Bluetooth Scan permission has been requested at least twice and
	 * user denied it, and checked 'Don't ask again'.
	 *
	 * @param activity the activity.
	 * @return True if permission has been denied and the popup will not come up any more,
	 * false otherwise.
	 */
	public static boolean isBluetoothScanPermissionDeniedForever(@NonNull final Activity activity) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);

		return !isLocationPermissionGranted(activity) // Location permission must be denied
				&& preferences.getBoolean(PREFS_BLUETOOTH_PERMISSION_REQUESTED, false) // Permission must have been requested before
				&& !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION); // This method should return false
	}

	/**
	 * Returns true if location permission has been requested at least twice and
	 * user denied it, and checked 'Don't ask again'.
	 *
	 * @param activity the activity.
	 * @return True if permission has been denied and the popup will not come up any more,
	 * false otherwise.
	 */
	public static boolean isLocationPermissionDeniedForever(@NonNull final Activity activity) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);

		return !isLocationPermissionGranted(activity) // Location permission must be denied
				&& preferences.getBoolean(PREFS_PERMISSION_REQUESTED, false) // Permission must have been requested before
				&& !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION); // This method should return false
	}

	/**
	 * On some devices running Android Marshmallow or newer location services must be enabled in
	 * order to scan for Bluetooth LE devices. This method returns whether the Location has been
	 * enabled or not.
	 *
	 * @return True on Android 6.0+ if location mode is different than LOCATION_MODE_OFF.
	 * It always returns true on Android versions prior to Marshmallow.
	 */
	public static boolean isLocationEnabled(@NonNull final Context context) {
		if (isMarshmallowOrAbove()) {
			LocationManager lm = context.getSystemService(LocationManager.class);
			return LocationManagerCompat.isLocationEnabled(lm);
		}
		return true;
	}

	/**
	 * Location enabled is required on some phones running Android 6 - 11
	 * (for example on Nexus and Pixel devices). Initially, Samsung phones didn't require it,
	 * but that has been fixed for those phones in Android 9.
	 *
	 * @param context the context.
	 * @return False if it is known that location is not required, true otherwise.
	 */
	public static boolean isLocationRequired(@NonNull final Context context) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		return preferences.getBoolean(PREFS_LOCATION_REQUIRED, isMarshmallowOrAbove() && !isSorAbove());
	}

	/**
	 * When a Bluetooth LE packet is received while Location is disabled it means that Location
	 * is not required on this device in order to scan for LE devices. This is a case of Samsung
	 * phones, for example. Save this information for the future to keep the Location info hidden.
	 *
	 * @param context the context.
	 */
	public static void markLocationNotRequired(@NonNull final Context context) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		preferences.edit().putBoolean(PREFS_LOCATION_REQUIRED, false).apply();
	}

	/**
	 * The first time an app requests a permission there is no 'Don't ask again' checkbox and
	 * {@link ActivityCompat#shouldShowRequestPermissionRationale(Activity, String)} returns false.
	 * This situation is similar to a permission being denied forever, so to distinguish both cases
	 * a flag needs to be saved.
	 *
	 * @param context the context.
	 */
	public static void markBluetoothScanPermissionRequested(@NonNull final Context context) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		preferences.edit().putBoolean(PREFS_BLUETOOTH_PERMISSION_REQUESTED, true).apply();
	}

	public static void clearBluetoothPermissionRequested(@NonNull final Context context) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		preferences.edit().putBoolean(PREFS_BLUETOOTH_PERMISSION_REQUESTED, false).apply();
	}

	/**
	 * The first time an app requests a permission there is no 'Don't ask again' checkbox and
	 * {@link ActivityCompat#shouldShowRequestPermissionRationale(Activity, String)} returns false.
	 * This situation is similar to a permission being denied forever, so to distinguish both cases
	 * a flag needs to be saved.
	 *
	 * @param context the context.
	 */
	public static void markLocationPermissionRequested(@NonNull final Context context) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		preferences.edit().putBoolean(PREFS_PERMISSION_REQUESTED, true).apply();
	}

	public static void clearLocationPermissionRequested(@NonNull final Context context) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		preferences.edit().putBoolean(PREFS_PERMISSION_REQUESTED, false).apply();
	}

	public static boolean isMarshmallowOrAbove() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
	}

	public static boolean isSorAbove() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
	}
}
