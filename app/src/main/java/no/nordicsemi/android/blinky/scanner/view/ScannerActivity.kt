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
package no.nordicsemi.android.blinky.scanner.view

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import no.nordicsemi.android.blinky.BlinkyActivity
import no.nordicsemi.android.blinky.LocalDataProvider
import no.nordicsemi.android.blinky.R
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice
import no.nordicsemi.android.blinky.databinding.ActivityScannerBinding
import no.nordicsemi.android.blinky.utils.Utils
import no.nordicsemi.android.blinky.scanner.viewmodel.ScannerState
import no.nordicsemi.android.blinky.scanner.viewmodel.ScannerViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ScannerActivity : AppCompatActivity(), DevicesAdapter.OnItemClickListener {

    private val scannerViewModel: ScannerViewModel by viewModel()
    private val dataProvider: LocalDataProvider by inject()
    private val utils: Utils by inject()
    private val binding by lazy { ActivityScannerBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        val toolbar = binding.toolbar
        toolbar.setTitle(R.string.app_name)
        setSupportActionBar(toolbar)

        scannerViewModel.scannerState.observe(this) {
            startScan(it)
        }

        // Configure the recycler view
        val recyclerView = binding.recyclerViewBleDevices
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        val animator = recyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
        val adapter = DevicesAdapter(this, scannerViewModel.devices)
        adapter.setOnItemClickListener(this)
        recyclerView.adapter = adapter

        // Set up permission request launcher
        val requestPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { result: Boolean? -> scannerViewModel.refresh() }
        val requestPermissions = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
            ActivityResultCallback<Map<String?, Boolean?>> { result: Map<String?, Boolean?>? -> scannerViewModel.refresh() }
        )

        // Configure views
        binding.noDevices.actionEnableLocation.setOnClickListener { v: View? -> openLocationSettings() }
        binding.bluetoothOff.actionEnableBluetooth.setOnClickListener { v: View? -> requestBluetoothEnabled() }
        binding.noLocationPermission.actionGrantLocationPermission.setOnClickListener { v: View? ->
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                dataProvider.locationPermissionRequested = true

            }
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        binding.noLocationPermission.actionPermissionSettings.setOnClickListener { v: View? ->
            dataProvider.locationPermissionRequested = false
            openPermissionSettings()
        }
        if (dataProvider.isSorAbove) {
            binding.noBluetoothPermission.actionGrantBluetoothPermission.setOnClickListener { v: View? ->
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                ) {
                    dataProvider.bluetoothPermissionRequested = true
                }
                requestPermissions.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
            }
            binding.noBluetoothPermission.actionPermissionSettings.setOnClickListener { v: View? ->
                dataProvider.bluetoothPermissionRequested = false
                openPermissionSettings()
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        clear()
    }

    override fun onStop() {
        super.onStop()
        stopScan()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.filter, menu)
        menu.findItem(R.id.filter_uuid).isChecked = dataProvider.uuidRequired
        menu.findItem(R.id.filter_nearby).isChecked = dataProvider.nearbyOnly
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.filter_uuid) {
            item.isChecked = !item.isChecked
            scannerViewModel.filterByUuid(item.isChecked)
            return true
        } else if (itemId == R.id.filter_nearby) {
            item.isChecked = !item.isChecked
            scannerViewModel.filterByDistance(item.isChecked)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(device: DiscoveredBluetoothDevice) {
        val controlBlinkIntent = Intent(this, BlinkyActivity::class.java)
        controlBlinkIntent.putExtra(BlinkyActivity.EXTRA_DEVICE, device)
        startActivity(controlBlinkIntent)
    }

    /**
     * Starts scanning for Bluetooth LE devices or displays a message based on the scanner state.
     */
    private fun startScan(state: ScannerState) {
        // First, check the Location permission.
        // This is required since Marshmallow up until Android 11 in order to scan for Bluetooth LE
        // devices.
        if (dataProvider.isLocationPermissionRequired && !utils.isLocationPermissionGranted()) {
            binding.noLocationPermission.root.visibility = View.VISIBLE
            binding.noBluetoothPermission.root.visibility = View.GONE
            binding.bluetoothOff.root.visibility = View.GONE
            binding.stateScanning.visibility = View.INVISIBLE
            binding.noDevices.root.visibility = View.GONE
            val deniedForever = utils.isLocationPermissionDeniedForever(this)
            binding.noLocationPermission.actionGrantLocationPermission.visibility =
                if (deniedForever) View.GONE else View.VISIBLE
            binding.noLocationPermission.actionPermissionSettings.visibility =
                if (deniedForever) View.VISIBLE else View.GONE
            return
        }
        binding.noLocationPermission.root.visibility = View.GONE

        if (dataProvider.isSorAbove && !utils.isBluetoothScanPermissionGranted()) {
            binding.noBluetoothPermission.root.visibility = View.VISIBLE
            binding.bluetoothOff.root.visibility = View.GONE
            binding.stateScanning.visibility = View.INVISIBLE
            binding.noDevices.root.visibility = View.GONE
            val deniedForever = utils.isBluetoothScanPermissionDeniedForever(this)
            binding.noBluetoothPermission.actionGrantBluetoothPermission.visibility =
                if (deniedForever) View.GONE else View.VISIBLE
            binding.noBluetoothPermission.actionPermissionSettings.visibility =
                if (deniedForever) View.VISIBLE else View.GONE
            return
        }


        binding.noBluetoothPermission.root.visibility = View.GONE

        if (!state.isBluetoothEnabled) {
            binding.bluetoothOff.root.visibility = View.VISIBLE
            binding.stateScanning.visibility = View.INVISIBLE
            binding.noDevices.root.visibility = View.GONE
            binding.noBluetoothPermission.root.visibility = View.GONE
            clear()
            return
        }

        binding.bluetoothOff.root.visibility = View.GONE

        // We are now OK to start scanning
        scannerViewModel.startScan()
        binding.stateScanning.visibility = View.VISIBLE
        if (!state.hasRecords) {
            binding.noDevices.root.visibility = View.VISIBLE
            if (!dataProvider.isLocationPermissionRequired || utils.isLocationEnabled()) {
                binding.noDevices.noLocation.visibility = View.INVISIBLE
            } else {
                binding.noDevices.noLocation.visibility = View.VISIBLE
            }
        } else {
            binding.noDevices.root.visibility = View.GONE
        }
        return
    }

    /**
     * Stops scanning for Bluetooth LE devices.
     */
    private fun stopScan() {
        scannerViewModel.stopScan()
    }

    /**
     * Clears the list of devices, which will notify the observer.
     */
    private fun clear() {
        scannerViewModel.devices.clear()
        scannerViewModel.scannerState.clearRecords()
    }

    /**
     * Opens application settings in Android Settings app.
     */
    private fun openPermissionSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    /**
     * Opens Location settings.
     */
    private fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    /**
     * Shows a prompt to the user to enable Bluetooth on the device.
     *
     * @implSpec On Android 12+ BLUETOOTH_CONNECT permission needs to be granted before calling
     * this method. Otherwise, the app would crash with [SecurityException].
     * @see BluetoothAdapter.ACTION_REQUEST_ENABLE
     */
    private fun requestBluetoothEnabled() {
        if (utils.isBluetoothConnectPermissionGranted()) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableIntent)
        }
    }
}
