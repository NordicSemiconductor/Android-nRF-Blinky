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

package no.nordicsemi.android.blinky.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import no.nordicsemi.android.blinky.R;
import no.nordicsemi.android.blinky.ScannerActivity;
import no.nordicsemi.android.blinky.databinding.DeviceItemBinding;
import no.nordicsemi.android.blinky.viewmodels.DevicesLiveData;

public class DevicesAdapter extends ListAdapter<DiscoveredBluetoothDevice, DevicesAdapter.ViewHolder> {
	private static final DiffUtil.ItemCallback<DiscoveredBluetoothDevice> DIFFER = new DeviceDiffCallback();
	private OnItemClickListener onItemClickListener;

	@FunctionalInterface
	public interface OnItemClickListener {
		void onItemClick(@NonNull final DiscoveredBluetoothDevice device);
	}

	public void setOnItemClickListener(@Nullable final OnItemClickListener listener) {
		onItemClickListener = listener;
	}

	public DevicesAdapter(@NonNull final ScannerActivity activity,
						  @NonNull final DevicesLiveData devicesLiveData) {
		super(DIFFER);
		setHasStableIds(true);
		devicesLiveData.observe(activity, this::submitList);
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
		final View layoutView = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.device_item, parent, false);
		return new ViewHolder(layoutView);
	}

	@Override
	public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
		holder.bind(getItem(position));
	}

	@Override
	public long getItemId(final int position) {
		return getItem(position).hashCode();
	}

	final class ViewHolder extends RecyclerView.ViewHolder {
		private final DeviceItemBinding binding;

		private ViewHolder(@NonNull final View view) {
			super(view);
			binding = DeviceItemBinding.bind(view);
			binding.deviceContainer.setOnClickListener(v -> {
				if (onItemClickListener != null) {
					final DiscoveredBluetoothDevice device = getItem(getBindingAdapterPosition());
					onItemClickListener.onItemClick(device);
				}
			});
		}

		private void bind(@NonNull final DiscoveredBluetoothDevice device) {
			final String deviceName = device.getName();

			if (!TextUtils.isEmpty(deviceName))
				binding.deviceName.setText(deviceName);
			else
				binding.deviceName.setText(R.string.unknown_device);
			binding.deviceAddress.setText(device.getAddress());
			final int rssiPercent = (int) (100.0f * (127.0f + device.getRssi()) / (127.0f + 20.0f));
			binding.rssi.setImageLevel(rssiPercent);
		}
	}
}
