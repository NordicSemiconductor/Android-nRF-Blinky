package no.nordicsemi.android.blinky;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import no.nordicsemi.android.blinky.adapter.DevicesAdapter;
import no.nordicsemi.android.blinky.adapter.ExtendedBluetoothDevice;
import no.nordicsemi.android.blinky.utils.Utils;
import no.nordicsemi.android.blinky.viewmodels.ScannerViewModel;

public class ScannerActivity extends AppCompatActivity implements DevicesAdapter.OnItemClickListener {
	private static final int REQUEST_ACCESS_COARSE_LOCATION = 1022; // random number

	private ScannerViewModel mScannerViewModel;
	private DevicesAdapter mDevicesAdapter;

	@BindView(R.id.state_scanning) View mScanningView;
	@BindView(R.id.no_devices)View mEmptyView;
	@BindView(R.id.no_location_permission) View mNoLocationPermissionView;
	@BindView(R.id.action_grant_location_permission) Button mGrantPermissionButton;
	@BindView(R.id.action_permission_settings) Button mPermissionSettingsButton;
	@BindView(R.id.no_location)	View mNoLocationView;
	@BindView(R.id.bluetooth_off) View mNoBluetoothView;

	@Override
	protected void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scanner);
		ButterKnife.bind(this);

		final Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setTitle(R.string.app_name);

		// Create the MainActivityViewModel containing the Utility methods for scanning
		mScannerViewModel = ViewModelProviders.of(this).get(ScannerViewModel.class);
		mScannerViewModel.getScanningState().observe(this, scanning -> mScanningView.setVisibility(scanning ? View.VISIBLE : View.INVISIBLE));
		mScannerViewModel.getBluetoothState().observe(this, enabled -> startScan());
		mScannerViewModel.getLocationServicesState().observe(this, enabled -> startScan());
		mScannerViewModel.getDevices().observe(this, devices -> mEmptyView.setVisibility(devices.isEmpty() ? View.VISIBLE : View.GONE));

		mDevicesAdapter = new DevicesAdapter(this, mScannerViewModel.getDevices());
		mDevicesAdapter.setOnItemClickListener(this);

		final RecyclerView recyclerView = findViewById(R.id.recycler_view_ble_devices);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		final DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
		recyclerView.addItemDecoration(dividerItemDecoration);
		recyclerView.setAdapter(mDevicesAdapter);
	}

	@Override
	protected void onStart() {
		super.onStart();
		startScan();
	}

	@Override
	protected void onStop() {
		super.onStop();
		stopScan();
	}

	@Override
	public void onItemClick(final ExtendedBluetoothDevice device) {
		final Intent controlBlinkIntent = new Intent(this, BlinkyActivity.class);
		controlBlinkIntent.putExtra(BlinkyActivity.EXTRA_DEVICE, device);
		startActivity(controlBlinkIntent);
	}

	@Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case REQUEST_ACCESS_COARSE_LOCATION:
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					startScan();
				} else {
					final boolean deniedForever = Utils.isLocationPermissionDeniedForever(this);
					mGrantPermissionButton.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
					mPermissionSettingsButton.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
				}
				break;
		}
	}

	@OnClick(R.id.action_enable_location)
	public void onEnableLocationClicked() {
		final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}

	@OnClick(R.id.action_enable_bluetooth)
	public void onEnableBluetoothClicked() {
		final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivity(enableIntent);
	}

	@OnClick(R.id.action_grant_location_permission)
	public void onGrantLocationPermissionClicked() {
		ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ACCESS_COARSE_LOCATION);
	}

	@OnClick(R.id.action_permission_settings)
	public void onPermissionSettingsClicked() {
		final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
		intent.setData(Uri.fromParts("package", getPackageName(), null));
		startActivity(intent);
	}

	/**
	 * Start scanning for bluetooth devices.
	 */
	private void startScan() {
		// First, check the Location permission. This is required on Marshmallow onwards in order to scan for Bluetooth LE devices.
		if (Utils.isLocationPermissionsGranted(this)) {
			mNoLocationPermissionView.setVisibility(View.GONE);

			// Bluetooth must be enabled
			if (Utils.isBleEnabled()) {
				mNoBluetoothView.setVisibility(View.GONE);

				// We are now OK to start scanning
				mScannerViewModel.startScan();

				if (mDevicesAdapter.isEmpty()) {
					mEmptyView.setVisibility(View.VISIBLE);

					if (!Utils.isLocationRequired(this) || Utils.isLocationEnabled(this)) {
						mNoLocationView.setVisibility(View.INVISIBLE);
					} else {
						mNoLocationView.setVisibility(View.VISIBLE);
					}
				} else {
					mEmptyView.setVisibility(View.GONE);
				}
			} else {
				mNoBluetoothView.setVisibility(View.VISIBLE);
				mEmptyView.setVisibility(View.GONE);
			}
		} else {
			mNoLocationPermissionView.setVisibility(View.VISIBLE);
			mNoBluetoothView.setVisibility(View.GONE);
			mEmptyView.setVisibility(View.GONE);

			final boolean deniedForever = Utils.isLocationPermissionDeniedForever(this);
			mGrantPermissionButton.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
			mPermissionSettingsButton.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
		}
	}

	/**
	 * stop scanning for bluetooth devices.
	 */
	private void stopScan() {
		mScannerViewModel.stopScan();
	}
}
