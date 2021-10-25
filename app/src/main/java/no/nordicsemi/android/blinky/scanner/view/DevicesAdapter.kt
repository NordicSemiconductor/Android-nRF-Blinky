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

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import no.nordicsemi.android.blinky.R
import no.nordicsemi.android.blinky.adapter.DiscoveredBluetoothDevice
import no.nordicsemi.android.blinky.databinding.DeviceItemBinding
import no.nordicsemi.android.blinky.scanner.viewmodel.DevicesLiveData

class DevicesAdapter(
    activity: ScannerActivity,
    devicesLiveData: DevicesLiveData
) : RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {

    private var devices: List<DiscoveredBluetoothDevice>? = null
    private var onItemClickListener: OnItemClickListener? = null

    fun interface OnItemClickListener {
        fun onItemClick(device: DiscoveredBluetoothDevice)
    }

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutView = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_item, parent, false)
        return ViewHolder(layoutView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices!![position]
        val deviceName = device.name
        if (!TextUtils.isEmpty(deviceName)) holder.binding.deviceName.text =
            deviceName else holder.binding.deviceName.setText(
            R.string.unknown_device
        )
        holder.binding.deviceAddress.text = device.address
        val rssiPercent = (100.0f * (127.0f + device.rssi) / (127.0f + 20.0f)).toInt()
        holder.binding.rssi.setImageLevel(rssiPercent)
    }

    override fun getItemId(position: Int): Long {
        return devices!![position].hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return devices?.size ?: 0
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding: DeviceItemBinding = DeviceItemBinding.bind(view)

        init {
            binding.deviceContainer.setOnClickListener { v: View? ->
                if (onItemClickListener != null) {
                    onItemClickListener!!.onItemClick(devices!![bindingAdapterPosition])
                }
            }
        }
    }

    init {
        setHasStableIds(true)
        devicesLiveData.observe(activity) {
            val result = DiffUtil.calculateDiff(
                DeviceDiffCallback(devices!!, it!!), false
            )
            devices = it
            result.dispatchUpdatesTo(this)
        }
    }
}