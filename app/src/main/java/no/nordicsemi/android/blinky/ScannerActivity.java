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

package no.nordicsemi.android.blinky;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.appbar.MaterialToolbar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import no.nordicsemi.android.blinky.adapter.DevicesAdapter;
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice;
import no.nordicsemi.android.blinky.databinding.ActivityScannerBinding;
import no.nordicsemi.android.blinky.utils.Utils;
import no.nordicsemi.android.blinky.viewmodels.ScannerStateLiveData;
import no.nordicsemi.android.blinky.viewmodels.ScannerViewModel;

public class ScannerActivity extends AppCompatActivity implements DevicesAdapter.OnItemClickListener {
    private ScannerViewModel scannerViewModel;
    private ActivityScannerBinding binding;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> scannerViewModel.refresh()
    );

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final MaterialToolbar toolbar = binding.toolbar;
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        // Create view model containing utility methods for scanning
        scannerViewModel = new ViewModelProvider(this).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(this, this::startScan);

        // Configure the recycler view
        final RecyclerView recyclerView = binding.recyclerViewBleDevices;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        final RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        final DevicesAdapter adapter = new DevicesAdapter(this, scannerViewModel.getDevices());
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);

        // Configure views
        binding.noDevices.actionEnableLocation.setOnClickListener(v -> {
            final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        });
        binding.bluetoothOff.actionEnableBluetooth.setOnClickListener(v -> {
            final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableIntent);
        });
        binding.noLocationPermission.actionGrantLocationPermission.setOnClickListener(v -> {
            Utils.markLocationPermissionRequested(this);
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        });
        binding.noLocationPermission.actionPermissionSettings.setOnClickListener(v -> {
            final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        clear();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopScan();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.filter, menu);
        menu.findItem(R.id.filter_uuid).setChecked(scannerViewModel.isUuidFilterEnabled());
        menu.findItem(R.id.filter_nearby).setChecked(scannerViewModel.isNearbyFilterEnabled());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.filter_uuid) {
            item.setChecked(!item.isChecked());
            scannerViewModel.filterByUuid(item.isChecked());
            return true;
        } else if (itemId == R.id.filter_nearby) {
            item.setChecked(!item.isChecked());
            scannerViewModel.filterByDistance(item.isChecked());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(@NonNull final DiscoveredBluetoothDevice device) {
        final Intent controlBlinkIntent = new Intent(this, BlinkyActivity.class);
        controlBlinkIntent.putExtra(BlinkyActivity.EXTRA_DEVICE, device);
        startActivity(controlBlinkIntent);
    }

    /**
     * Start scanning for Bluetooth devices or displays a message based on the scanner state.
     */
    private void startScan(@NonNull final ScannerStateLiveData state) {
        // First, check the Location permission. This is required on Marshmallow onwards in order
        // to scan for Bluetooth LE devices.
        if (Utils.isLocationPermissionsGranted(this)) {
            binding.noLocationPermission.container.setVisibility(View.GONE);

            // Bluetooth must be enabled.
            if (state.isBluetoothEnabled()) {
                binding.bluetoothOff.container.setVisibility(View.GONE);

                // We are now OK to start scanning.
                scannerViewModel.startScan();
                binding.stateScanning.setVisibility(View.VISIBLE);

                if (!state.hasRecords()) {
                    binding.noDevices.container.setVisibility(View.VISIBLE);

                    if (!Utils.isLocationRequired(this) || Utils.isLocationEnabled(this)) {
                        binding.noDevices.noLocation.setVisibility(View.INVISIBLE);
                    } else {
                        binding.noDevices.noLocation.setVisibility(View.VISIBLE);
                    }
                } else {
                    binding.noDevices.container.setVisibility(View.GONE);
                }
            } else {
                binding.bluetoothOff.container.setVisibility(View.VISIBLE);
                binding.stateScanning.setVisibility(View.INVISIBLE);
                binding.noDevices.container.setVisibility(View.GONE);
                clear();
            }
        } else {
            binding.noLocationPermission.container.setVisibility(View.VISIBLE);
            binding.bluetoothOff.container.setVisibility(View.GONE);
            binding.stateScanning.setVisibility(View.INVISIBLE);
            binding.noDevices.container.setVisibility(View.GONE);

            final boolean deniedForever = Utils.isLocationPermissionDeniedForever(this);
            binding.noLocationPermission.actionGrantLocationPermission.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
            binding.noLocationPermission.actionPermissionSettings.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * stop scanning for bluetooth devices.
     */
    private void stopScan() {
        scannerViewModel.stopScan();
    }

    /**
     * Clears the list of devices, which will notify the observer.
     */
    private void clear() {
        scannerViewModel.getDevices().clear();
        scannerViewModel.getScannerState().clearRecords();
    }
}
