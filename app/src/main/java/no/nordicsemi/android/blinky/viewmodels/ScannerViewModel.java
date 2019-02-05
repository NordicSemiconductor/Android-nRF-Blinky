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

package no.nordicsemi.android.blinky.viewmodels;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.preference.PreferenceManager;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import no.nordicsemi.android.blinky.utils.Utils;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class ScannerViewModel extends AndroidViewModel {
	private static final String PREFS_FILTER_UUID_REQUIRED = "filter_uuid";
	private static final String PREFS_FILTER_NEARBY_ONLY = "filter_nearby";

	/**
	 * MutableLiveData containing the list of devices.
	 */
	private final DevicesLiveData mDevicesLiveData;
	/**
	 * MutableLiveData containing the scanner state.
	 */
	private final ScannerStateLiveData mScannerStateLiveData;

	private final SharedPreferences mPreferences;

	public DevicesLiveData getDevices() {
		return mDevicesLiveData;
	}

	public ScannerStateLiveData getScannerState() {
		return mScannerStateLiveData;
	}

	public ScannerViewModel(final Application application) {
		super(application);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(application);

		final boolean filterUuidRequired = isUuidFilterEnabled();
		final boolean filerNearbyOnly = isNearbyFilterEnabled();

		mScannerStateLiveData = new ScannerStateLiveData(Utils.isBleEnabled(),
				Utils.isLocationEnabled(application));
		mDevicesLiveData = new DevicesLiveData(filterUuidRequired, filerNearbyOnly);
		registerBroadcastReceivers(application);
	}

	@Override
	protected void onCleared() {
		super.onCleared();
		getApplication().unregisterReceiver(mBluetoothStateBroadcastReceiver);

		if (Utils.isMarshmallowOrAbove()) {
			getApplication().unregisterReceiver(mLocationProviderChangedReceiver);
		}
	}

	public boolean isUuidFilterEnabled() {
		return mPreferences.getBoolean(PREFS_FILTER_UUID_REQUIRED, true);
	}

	public boolean isNearbyFilterEnabled() {
		return mPreferences.getBoolean(PREFS_FILTER_NEARBY_ONLY, false);
	}

	/**
	 * Forces the observers to be notified. This method is used to refresh the screen after the
	 * location permission has been granted. In result, the observer in
	 * {@link no.nordicsemi.android.blinky.ScannerActivity} will try to start scanning.
	 */
	public void refresh() {
		mScannerStateLiveData.refresh();
	}

	/**
	 * Updates the device filter. Devices that once passed the filter will still be shown
	 * even if they move away from the phone, or change the advertising packet. This is to
	 * avoid removing devices from the list.
	 *
	 * @param uuidRequired if true, the list will display only devices with Led-Button Service UUID
	 *                     in the advertising packet.
	 */
	public void filterByUuid(final boolean uuidRequired) {
		mPreferences.edit().putBoolean(PREFS_FILTER_UUID_REQUIRED, uuidRequired).apply();
		if (mDevicesLiveData.filterByUuid(uuidRequired))
			mScannerStateLiveData.recordFound();
		else
			mScannerStateLiveData.clearRecords();
	}

	/**
	 * Updates the device filter. Devices that once passed the filter will still be shown
	 * even if they move away from the phone, or change the advertising packet. This is to
	 * avoid removing devices from the list.
	 *
	 * @param nearbyOnly if true, the list will show only devices with high RSSI.
	 */
	public void filterByDistance(final boolean nearbyOnly) {
		mPreferences.edit().putBoolean(PREFS_FILTER_NEARBY_ONLY, nearbyOnly).apply();
		if (mDevicesLiveData.filterByDistance(nearbyOnly))
			mScannerStateLiveData.recordFound();
		else
			mScannerStateLiveData.clearRecords();
	}

	/**
	 * Start scanning for Bluetooth devices.
	 */
	public void startScan() {
		if (mScannerStateLiveData.isScanning()) {
			return;
		}

		// Scanning settings
		final ScanSettings settings = new ScanSettings.Builder()
				.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
				.setReportDelay(500)
				.setUseHardwareBatchingIfSupported(false)
				.build();

		final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
		scanner.startScan(null, settings, scanCallback);
		mScannerStateLiveData.scanningStarted();
	}

	/**
	 * Stop scanning for bluetooth devices.
	 */
	public void stopScan() {
		final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
		scanner.stopScan(scanCallback);
		mScannerStateLiveData.scanningStopped();
	}

	private final ScanCallback scanCallback = new ScanCallback() {
		@Override
		public void onScanResult(final int callbackType, @NonNull final ScanResult result) {
			// This callback will be called only if the scan report delay is not set or is set to 0.

			// If the packet has been obtained while Location was disabled, mark Location as not required
			if (Utils.isLocationRequired(getApplication()) && !Utils.isLocationEnabled(getApplication()))
				Utils.markLocationNotRequired(getApplication());

			if (mDevicesLiveData.deviceDiscovered(result)) {
				mDevicesLiveData.applyFilter();
				mScannerStateLiveData.recordFound();
			}
		}

		@Override
		public void onBatchScanResults(@NonNull final List<ScanResult> results) {
			// This callback will be called only if the report delay set above is greater then 0.

			// If the packet has been obtained while Location was disabled, mark Location as not required
			if (Utils.isLocationRequired(getApplication()) && !Utils.isLocationEnabled(getApplication()))
				Utils.markLocationNotRequired(getApplication());

			boolean atLeastOneMatchedFilter = false;
			for (final ScanResult result : results)
				atLeastOneMatchedFilter = mDevicesLiveData.deviceDiscovered(result) || atLeastOneMatchedFilter;
			if (atLeastOneMatchedFilter) {
				mDevicesLiveData.applyFilter();
				mScannerStateLiveData.recordFound();
			}
		}

		@Override
		public void onScanFailed(final int errorCode) {
			// TODO This should be handled
			mScannerStateLiveData.scanningStopped();
		}
	};

	/**
	 * Register for required broadcast receivers.
	 */
	private void registerBroadcastReceivers(final Application application) {
		application.registerReceiver(mBluetoothStateBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
		if (Utils.isMarshmallowOrAbove()) {
			application.registerReceiver(mLocationProviderChangedReceiver, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
		}
	}

	/**
	 * Broadcast receiver to monitor the changes in the location provider.
	 */
	private final BroadcastReceiver mLocationProviderChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final boolean enabled = Utils.isLocationEnabled(context);
			mScannerStateLiveData.setLocationEnabled(enabled);
		}
	};

	/**
	 * Broadcast receiver to monitor the changes in the bluetooth adapter.
	 */
	private final BroadcastReceiver mBluetoothStateBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
			final int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);

			switch (state) {
				case BluetoothAdapter.STATE_ON:
					mScannerStateLiveData.bluetoothEnabled();
					break;
				case BluetoothAdapter.STATE_TURNING_OFF:
				case BluetoothAdapter.STATE_OFF:
					if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
						stopScan();
						mScannerStateLiveData.bluetoothDisabled();
					}
					break;
			}
		}
	};
}
