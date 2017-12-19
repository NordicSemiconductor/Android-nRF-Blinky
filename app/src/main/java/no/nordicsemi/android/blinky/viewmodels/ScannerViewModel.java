/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.blinky.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.blinky.profile.BlinkyManager;
import no.nordicsemi.android.blinky.utils.Utils;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class ScannerViewModel extends AndroidViewModel {

	/** LiveData object to notify the scanning state changes to MainActivity. */
	private final MutableLiveData<Boolean> mScanningLiveData = new MutableLiveData<>();

	/** Live data object to notify the Bluetooth state to MainActivity. */
	private final MutableLiveData<Boolean> mBluetoothStateLiveData = new MutableLiveData<>();

	/** MutableLiveData containing changes to location service provider to notify MainActivity. */
	private final MutableLiveData<Boolean> mLocationServicesStateLiveData = new MutableLiveData<>();

	/** MutableLiveData BleDeviceAdapter containing BLE devices to notify MainActivity. */
	private final DevicesLiveData mDevicesLiveData = new DevicesLiveData();

	public ScannerViewModel(final Application application) {
		super(application);

		mScanningLiveData.setValue(false);
		registerBroadcastReceivers(application);
	}

	public LiveData<Boolean> getScanningState() {
		return mScanningLiveData;
	}

	public LiveData<Boolean> getBluetoothState() {
		return mBluetoothStateLiveData;
	}

	public LiveData<Boolean> getLocationServicesState() {
		return mLocationServicesStateLiveData;
	}

	public DevicesLiveData getDevices() {
		return mDevicesLiveData;
	}

	@Override
	protected void onCleared() {
		super.onCleared();
		getApplication().unregisterReceiver(mBluetoothStateBroadcastReceiver);

		if (Utils.isMarshmallowOrAbove()) {
			getApplication().unregisterReceiver(mLocationProviderChangedReceiver);
		}
	}

	/**
	 * Start scanning for bluetooth devices.
	 */
	public void startScan() {
		if (mScanningLiveData.getValue()) {
			return;
		}

		// Scanning settings
		final ScanSettings settings = new ScanSettings.Builder()
				.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
				// Refresh the devices list every second
				.setReportDelay(0)
				// Hardware filtering has some issues on selected devices
				.setUseHardwareFilteringIfSupported(false)
				// Samsung S6 and S6 Edge report equal value of RSSI for all devices. In this app we ignore the RSSI.
					/*.setUseHardwareBatchingIfSupported(false)*/
				.build();

		// Let's use the filter to scan only for Blinky devices
		final ParcelUuid uuid = new ParcelUuid(BlinkyManager.LBS_UUID_SERVICE);
		final List<ScanFilter> filters = new ArrayList<>();
		filters.add(new ScanFilter.Builder().setServiceUuid(uuid).build());

		final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
		scanner.startScan(filters, settings, scanCallback);
		mScanningLiveData.setValue(true);
	}

	/**
	 * stop scanning for bluetooth devices.
	 */
	public void stopScan() {
		final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
		scanner.stopScan(scanCallback);
		mScanningLiveData.setValue(false);
	}

	private final ScanCallback scanCallback = new ScanCallback() {
		@Override
		public void onScanResult(final int callbackType, final ScanResult result) {
			// If the packet has been obtained while Location was disabled, mark Location as not required
			if (Utils.isLocationRequired(getApplication()) && !Utils.isLocationEnabled(getApplication()))
				Utils.markLocationNotRequired(getApplication());

			mDevicesLiveData.onDeviceDiscovered(result);
		}

		@Override
		public void onBatchScanResults(final List<ScanResult> results) {
			// Batch scan is disabled (report delay = 0)
		}

		@Override
		public void onScanFailed(final int errorCode) {
			// TODO This should be handled
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
	 * Broadcast receiver to monitor the changes in the location provider
	 */
	private final BroadcastReceiver mLocationProviderChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final boolean enabled = Utils.isLocationEnabled(context);
			mLocationServicesStateLiveData.postValue(enabled);
		}
	};

	/**
	 * Broadcast receiver to monitor the changes in the bluetooth adapter
	 */
	private final BroadcastReceiver mBluetoothStateBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
			final int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);

			switch (state) {
				case BluetoothAdapter.STATE_ON:
					mBluetoothStateLiveData.postValue(true);
					break;
				case BluetoothAdapter.STATE_TURNING_OFF:
				case BluetoothAdapter.STATE_OFF:
					if (previousState != BluetoothAdapter.STATE_TURNING_OFF && previousState != BluetoothAdapter.STATE_OFF) {
						stopScan();
						mDevicesLiveData.clear();
						mBluetoothStateLiveData.postValue(false);
					}
					break;
			}
		}
	};
}
