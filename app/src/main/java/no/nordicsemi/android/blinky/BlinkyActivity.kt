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
package no.nordicsemi.android.blinky

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.ble.observer.ConnectionObserver
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice
import no.nordicsemi.android.blinky.databinding.ActivityBlinkyBinding
import no.nordicsemi.android.blinky.viewmodels.BlinkyViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class BlinkyActivity : AppCompatActivity() {

    private val viewModel: BlinkyViewModel by viewModel()

    private val binding: ActivityBlinkyBinding by lazy { ActivityBlinkyBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        val intent = intent
        val device: DiscoveredBluetoothDevice = intent.getParcelableExtra(EXTRA_DEVICE)!!
        val deviceName = device.name
        val deviceAddress = device.address
        val toolbar = binding.toolbar
        toolbar.title = deviceName ?: getString(R.string.unknown_device)
        toolbar.subtitle = deviceAddress
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Configure the view model.
        viewModel.connect(device)

        // Set up views.
        binding.ledSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            viewModel.setLedState(
                isChecked
            )
        }
        binding.infoNotSupported.actionRetry.setOnClickListener { v: View? -> viewModel.reconnect() }
        binding.infoTimeout.actionRetry.setOnClickListener { v: View? -> viewModel.reconnect() }
        viewModel.connectionState.observe(this, { state: ConnectionState ->
            when (state.state) {
                ConnectionState.State.CONNECTING -> {
                    binding.progressContainer.visibility = View.VISIBLE
                    binding.infoNotSupported.container.visibility = View.GONE
                    binding.infoTimeout.container.visibility = View.GONE
                    binding.connectionState.setText(R.string.state_connecting)
                }
                ConnectionState.State.INITIALIZING -> binding.connectionState.setText(R.string.state_initializing)
                ConnectionState.State.READY -> {
                    binding.progressContainer.visibility = View.GONE
                    binding.deviceContainer.visibility = View.VISIBLE
                    onConnectionStateChanged(true)
                }
                ConnectionState.State.DISCONNECTED -> {
                    if (state is ConnectionState.Disconnected) {
                        binding.deviceContainer.visibility = View.GONE
                        binding.progressContainer.visibility = View.GONE
                        if (state.reason == ConnectionObserver.REASON_NOT_SUPPORTED) {
                            binding.infoNotSupported.container.visibility = View.VISIBLE
                        } else {
                            binding.infoTimeout.container.visibility = View.VISIBLE
                        }
                    }
                    onConnectionStateChanged(false)
                }
                ConnectionState.State.DISCONNECTING -> onConnectionStateChanged(false)
                null -> TODO()
            }
        })
        viewModel.ledState.observe(this, { isOn: Boolean ->
            binding.ledState.setText(if (isOn) R.string.turn_on else R.string.turn_off)
            binding.ledSwitch.isChecked = isOn
        })
        viewModel.buttonState.observe(this,
            { pressed: Boolean -> binding.buttonState.setText(if (pressed) R.string.button_pressed else R.string.button_released) })
    }

    private fun onConnectionStateChanged(connected: Boolean) {
        binding.ledSwitch.isEnabled = connected
        if (!connected) {
            binding.ledSwitch.isChecked = false
            binding.buttonState.setText(R.string.button_unknown)
        }
    }

    companion object {
        const val EXTRA_DEVICE = "no.nordicsemi.android.blinky.EXTRA_DEVICE"
    }
}