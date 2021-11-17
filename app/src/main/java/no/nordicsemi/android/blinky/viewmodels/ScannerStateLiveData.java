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

import androidx.lifecycle.LiveData;

/**
 * This class keeps the current state of the scanner.
 */
@SuppressWarnings("unused")
public class ScannerStateLiveData extends LiveData<ScannerStateLiveData> {
	private boolean scanningStarted;
	private boolean hasRecords;
	private boolean bluetoothEnabled;
	private boolean locationEnabled;

	/* package */ ScannerStateLiveData(final boolean bluetoothEnabled,
									   final boolean locationEnabled) {
		this.scanningStarted = false;
		this.bluetoothEnabled = bluetoothEnabled;
		this.locationEnabled = locationEnabled;
	}

	/* package */ void refresh() {
		postValue(this);
	}

	/* package */ void scanningStarted() {
		scanningStarted = true;
		postValue(this);
	}

	/* package */ void scanningStopped() {
		scanningStarted = false;
		postValue(this);
	}

	/* package */ void bluetoothEnabled() {
		bluetoothEnabled = true;
		postValue(this);
	}

	/* package */ synchronized void bluetoothDisabled() {
		bluetoothEnabled = false;
		hasRecords = false;
		postValue(this);
	}

	/* package */ void setLocationEnabled(final boolean enabled) {
		locationEnabled = enabled;
		postValue(this);
	}

	/**
	 * Notifies observers that a record has been found.
	 */
	/* package */ void recordFound() {
		if (!hasRecords) {
			hasRecords = true;
			postValue(this);
		}
	}

	/**
	 * Notifies observers that scanner has no records to show.
	 */
	/* package */ void clearRecords() {
		if (hasRecords) {
			hasRecords = false;
			postValue(this);
		}
	}

	/**
	 * Returns whether scanning is in progress.
	 */
	/* package */ boolean isScanning() {
		return scanningStarted;
	}

	/**
	 * Returns whether any records matching filter criteria has been found.
	 */
	public boolean hasRecords() {
		return hasRecords;
	}

	/**
	 * Returns whether Bluetooth adapter is enabled.
	 */
	public boolean isBluetoothEnabled() {
		return bluetoothEnabled;
	}

	/**
	 * Returns whether Location is enabled.
	 */
	public boolean isLocationEnabled() {
		return locationEnabled;
	}
}
