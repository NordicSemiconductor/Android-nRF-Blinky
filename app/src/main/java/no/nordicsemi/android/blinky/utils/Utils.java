package no.nordicsemi.android.blinky.utils;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class Utils {
	private static final String PREFS_LOCATION_NOT_REQUIRED = "location_not_required";

	/**
	 * Checks whether Bluetooth is enabled.
	 * @return true if Bluetooth is enabled, false otherwise.
	 */
	public static boolean isBleEnabled() {
		final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		return adapter != null && adapter.isEnabled();
	}

	/**
	 * Checks for required permissions.
	 *
	 * @return true if permissions are already granted, false otherwise.
	 */
	public static boolean isLocationPermissionsGranted(final Context context) {
		return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
	}

	public static boolean isLocationPermissionDeniedForever(final Activity activity) {
		return !isLocationPermissionsGranted(activity) && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION);
	}

	/**
	 * On some devices running Android Marshmallow or newer location services must be enabled in order to scan for Bluetooth LE devices.
	 * This method returns whether the Location has been enabled or not.
	 *
	 * @return true on Android 6.0+ if location mode is different than LOCATION_MODE_OFF. It always returns true on Android versions prior to Marshmallow.
	 */
	public static boolean isLocationEnabled(final Context context) {
		if (isMarshmallowOrAbove()) {
			int locationMode = Settings.Secure.LOCATION_MODE_OFF;
			try {
				locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
			} catch (final Settings.SettingNotFoundException e) {
				// do nothing
			}
			return locationMode != Settings.Secure.LOCATION_MODE_OFF;
		}
		return true;
	}

	/**
	 * Location enabled is required on some phones running Android Marshmallow or newer (for example on Nexus and Pixel devices).
	 *
	 * @param context the context
	 * @return false if it is known that location is not required, true otherwise
	 */
	public static boolean isLocationRequired(final Context context) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		return preferences.getBoolean(PREFS_LOCATION_NOT_REQUIRED, isMarshmallowOrAbove());
	}

	/**
	 * When a Bluetooth LE packet is received while Location is disabled it means that Location
	 * is not required on this device in order to scan for LE devices. This is a case of Samsung phones, for example.
	 * Save this information for the future to keep the Location info hidden.
	 * @param context the context
	 */
	public static void markLocationNotRequired(final Context context) {
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		preferences.edit().putBoolean(PREFS_LOCATION_NOT_REQUIRED, false).apply();
	}

	public static boolean isMarshmallowOrAbove() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
	}
}
