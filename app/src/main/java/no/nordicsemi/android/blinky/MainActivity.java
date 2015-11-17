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

package no.nordicsemi.android.blinky;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.blinky.adapter.BleDeviceAdapter;
import no.nordicsemi.android.blinky.adapter.ExtendedBluetoothDevice;
import no.nordicsemi.android.blinky.service.BlinkyService;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class MainActivity extends AppCompatActivity implements PermissionRationaleFragment.PermissionDialogListener,
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, AdapterView.OnItemClickListener {
	private static final String TAG = "MainActivity";

	private static final int REQUEST_ENABLE_BT = 0;
	private static final int REQUEST_EMPTY = 210;
	private static final int REQUEST_LOCATION_SERVICES = 211;
	private static final int REQUEST_ACCESS_COARSE_LOCATION = 212;

	private static final long SCAN_PERIOD = 10000; // [ms]

	/** LED Button Service UUID that's required in the device's Advertising packet to be shown. */
	private final static String LBS_UUID_SERVICE = "00001523-1212-efde-1523-785feabcd123";

	private BluetoothLeScannerCompat mScanner;
	private ArrayList<ScanFilter> scanFilterList;
	private Handler mScannerHandler;
	private BleDeviceAdapter mBleDeviceListAdapter;
	private View mParentView;
	private GoogleApiClient mGoogleApiClient = null;
	private boolean locationServicesRequestApproved = false;
	private boolean mScanning;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setTitle(R.string.app_name);

		mParentView = findViewById(R.id.container);
		mScannerHandler = new Handler();

		final ListView listBleDevices = (ListView) findViewById(R.id.list_view_ble_devices);
		listBleDevices.setAdapter(mBleDeviceListAdapter = new BleDeviceAdapter());
		listBleDevices.setOnItemClickListener(this);

		prepareForScan();
	}

	@Override
	public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
		BluetoothDevice device = mBleDeviceListAdapter.getItem(position);
		Intent controlBlinkIntent = new Intent(this, ControlBlinkyActivity.class);
		controlBlinkIntent.putExtra(BlinkyService.EXTRA_DEVICE_NAME, device.getName());
		controlBlinkIntent.putExtra(BlinkyService.EXTRA_DEVICE_ADDRESS, device.getAddress());
		startActivity(controlBlinkIntent);

		mBleDeviceListAdapter.clear();
		mBleDeviceListAdapter.notifyDataSetChanged();
	}

	private void prepareForScan() {
		if (isBleSupported()) {
			final ParcelUuid uuid = ParcelUuid.fromString(LBS_UUID_SERVICE);
			scanFilterList = new ArrayList<>();
			scanFilterList.add(new ScanFilter.Builder().setServiceUuid(uuid).build());
			mScanner = BluetoothLeScannerCompat.getScanner();
		} else {
			showError(getString(R.string.ble_not_supported), false);
		}
	}

	private void connectToGoogleApiClient() {
		if (mGoogleApiClient == null) {
			mGoogleApiClient = new GoogleApiClient.Builder(this)
					.addApi(LocationServices.API)
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.build();
		}
		if (!mGoogleApiClient.isConnected())
			mGoogleApiClient.connect();
		else
			createLocationRequestForResult();
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (checkIfVersionIsMarshmallowOrAbove()) {
			registerReceiver(mLocationProviderChangedReceiver, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
			connectToGoogleApiClient();
		} else {
			if (!isBleEnabled()) {
				final Intent bluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(bluetoothEnable, REQUEST_ENABLE_BT);
			} else {
				startLeScan();
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		if (mScanning)
			stopLeScan();

		if (checkIfVersionIsMarshmallowOrAbove()) {
			unregisterReceiver(mLocationProviderChangedReceiver);
			disconnectFromGoogleApiClient();
		}
	}

	private void disconnectFromGoogleApiClient() {
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		if (!isBleSupported())
			return false;

		// Inflate the menu; this adds items to the action bar if it is present.
		if (mScanning)
			getMenuInflater().inflate(R.menu.menu_stop_scan, menu);
		else
			getMenuInflater().inflate(R.menu.menu_start_scan, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
			case R.id.action_start_scan:
				if (checkIfVersionIsMarshmallowOrAbove()) {
					if (locationServicesRequestApproved)
						checkForLocationPermissionsAndScan();
					else
						createLocationRequestForResult();
				} else {
					if (!isBleEnabled()) {
						final Intent bluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						startActivityForResult(bluetoothEnable, REQUEST_ENABLE_BT);
					} else {
						startLeScan();
					}
				}
				return true;
			case R.id.action_stop_scan:
				stopLeScan();
				return true;
			case android.R.id.home:
				onBackPressed();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void checkForLocationPermissionsAndScan() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			if (!isBleEnabled()) {
				final Intent bluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(bluetoothEnable, REQUEST_ENABLE_BT);
			} else {
				startLeScan();
			}
		} else {
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
				showPermissionRationaleFragment(R.string.rationale_location_message, REQUEST_ACCESS_COARSE_LOCATION);
				return;
			}
			onRequestPermission(REQUEST_ACCESS_COARSE_LOCATION);
		}
	}

	@Override
	public void onRequestPermission(final int permissionType) {
		switch (permissionType) {
			case REQUEST_ACCESS_COARSE_LOCATION:
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ACCESS_COARSE_LOCATION);
				break;
			case REQUEST_LOCATION_SERVICES:
				connectToGoogleApiClient();
				break;
		}
	}

	@Override
	public void onCancelRequestPermission() {
		showError(getString(R.string.rationale_location_cancel_message), true);
	}

	private void showPermissionRationaleFragment(final int resId, final int permissionType) {
		final PermissionRationaleFragment permissionFrag = PermissionRationaleFragment.getInstance(resId, permissionType);
		permissionFrag.show(getSupportFragmentManager(), null);
	}

	private void createLocationRequestForResult() {
		final LocationRequest locationRequestBalancedPowerAccuracy = new LocationRequest();
		final LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
				.addLocationRequest(locationRequestBalancedPowerAccuracy)
				.setAlwaysShow(true);
		final PendingResult<LocationSettingsResult> result =
				LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
		result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
			@Override
			public void onResult(final LocationSettingsResult locationSettingsResult) {
				final LocationSettingsStates states = locationSettingsResult.getLocationSettingsStates();
				if (states.isLocationUsable()) {
					checkForLocationPermissionsAndScan();
					return;
				}

				final Status status = locationSettingsResult.getStatus();
				switch (status.getStatusCode()) {
					case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
						locationServicesRequestApproved = false;
						try {
							status.startResolutionForResult(MainActivity.this, REQUEST_LOCATION_SERVICES);
						} catch (final IntentSender.SendIntentException e) {
							Log.e(TAG, "Exception occurred", e);
						}
						break;
					case LocationSettingsStatusCodes.SUCCESS:
						locationServicesRequestApproved = true;
						checkForLocationPermissionsAndScan();
						break;
					case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
						showPermissionRationaleFragment(R.string.rationale_location_cancel_message, REQUEST_EMPTY);
						break;
				}
			}
		});
	}

	@Override
	public void onRequestPermissionsResult(final int requestCode, final @NonNull String[] permissions, final @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case REQUEST_ACCESS_COARSE_LOCATION:
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					checkForLocationPermissionsAndScan();
				} else {
					showError(getString(R.string.rationale_location_permission_denied), true);
				}
				break;
			case REQUEST_EMPTY:
				break;
		}
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case REQUEST_ENABLE_BT:
				if (resultCode == RESULT_OK) {
					startLeScan();
				}
				break;
			case REQUEST_LOCATION_SERVICES:
				if (resultCode == RESULT_OK) {
					if (!mScanning) {
						locationServicesRequestApproved = true;
						checkForLocationPermissionsAndScan();
					}
				} else {
					showPermissionRationaleFragment(R.string.rationale_location_message, REQUEST_LOCATION_SERVICES);
				}
				break;
		}
	}

	private void startLeScan() {
		final ScanSettings settings = new ScanSettings.Builder()
				.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
						// Refresh the devices list every second
				.setReportDelay(1000)
						// Hardware filtering has some issues on selected devices
				.setUseHardwareFilteringIfSupported(false)
						// Samsung S6 and S6 Edge report equal value of RSSI for all devices. In this app we ignore the RSSI.
					/*.setUseHardwareBatchingIfSupported(false)*/
				.build();

		// Clear the devices list
		mBleDeviceListAdapter.clear();
		mBleDeviceListAdapter.notifyDataSetChanged();

		mScannerHandler.postDelayed(mStopScanningTask, SCAN_PERIOD);
		mScanner.startScan(scanFilterList, settings, scanCallback);
		mScanning = true;
		invalidateOptionsMenu();
	}

	private void stopLeScan() {
		mScannerHandler.removeCallbacks(mStopScanningTask);
		mScanning = false;
		mScanner.stopScan(scanCallback);
		invalidateOptionsMenu();
	}

	private Runnable mStopScanningTask = new Runnable() {
		@Override
		public void run() {
			stopLeScan();
		}
	};

	private ScanCallback scanCallback = new ScanCallback() {
		@Override
		public void onScanResult(final int callbackType, final ScanResult result) {
			// We scan with report delay > 0. This will never be called.
		}

		@Override
		public void onBatchScanResults(final List<ScanResult> results) {
			boolean newDeviceFound = false;
			for (final ScanResult result : results) {
				if (!mBleDeviceListAdapter.hasDevice(result)) {
					newDeviceFound = true;
					mBleDeviceListAdapter.addDevice(new ExtendedBluetoothDevice(result));
				}
			}

			if (newDeviceFound)
				mBleDeviceListAdapter.notifyDataSetChanged();
		}

		@Override
		public void onScanFailed(final int errorCode) {
			// This should be handled
		}
	};

	private void showError(final String error, boolean showAction) {
		final Snackbar snackbar = Snackbar.make(mParentView, error, Snackbar.LENGTH_LONG);
		if (showAction)
			snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorPrimary)).setAction(R.string.action_settings, new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					startActivity(intent);
				}
			});
		snackbar.show();
	}

	@Override
	public void onConnected(final Bundle bundle) {
		createLocationRequestForResult();
	}

	@Override
	public void onConnectionSuspended(final int reason) {
		// do nothing
	}

	@Override
	public void onConnectionFailed(final ConnectionResult connectionResult) {
		// do nothing
	}

	public boolean isLocationEnabled() {
		if (checkIfVersionIsMarshmallowOrAbove()) {
			int locationMode = Settings.Secure.LOCATION_MODE_OFF;

			try {
				locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
			} catch (final Settings.SettingNotFoundException e) {
				// do nothing
			}
			return locationMode != Settings.Secure.LOCATION_MODE_OFF;
		}
		return true;
	}

	final BroadcastReceiver mLocationProviderChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (!isLocationEnabled()) {
				stopLeScan();
			}
		}
	};

	private boolean checkIfVersionIsMarshmallowOrAbove() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
	}

	private boolean isBleEnabled() {
		final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		return adapter != null && adapter.isEnabled();
	}

	private boolean isBleSupported() {
		return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
	}
}
