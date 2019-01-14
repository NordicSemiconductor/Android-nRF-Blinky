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

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class DeviceDiffCallback extends DiffUtil.Callback {
	private final List<DiscoveredBluetoothDevice> oldList;
	private final List<DiscoveredBluetoothDevice> newList;

	DeviceDiffCallback(final List<DiscoveredBluetoothDevice> oldList,
					   final List<DiscoveredBluetoothDevice> newList) {
		this.oldList = oldList;
		this.newList = newList;
	}

	@Override
	public int getOldListSize() {
		return oldList != null ? oldList.size() : 0;
	}

	@Override
	public int getNewListSize() {
		return newList != null ? newList.size() : 0;
	}

	@Override
	public boolean areItemsTheSame(final int oldItemPosition, final int newItemPosition) {
		return oldList.get(oldItemPosition) == newList.get(newItemPosition);
	}

	@Override
	public boolean areContentsTheSame(final int oldItemPosition, final int newItemPosition) {
		final DiscoveredBluetoothDevice device = oldList.get(oldItemPosition);
		return device.hasRssiLevelChanged();
	}
}
