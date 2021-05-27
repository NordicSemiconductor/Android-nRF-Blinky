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

package no.nordicsemi.android.blinky.profile;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.common.callback.battery.BatteryLevelDataCallback;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.livedata.ObservableBleManager;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;
import no.nordicsemi.android.ble.common.callback.hr.HeartRateMeasurementDataCallback;


public class BlinkyManager extends ObservableBleManager {

	public static final UUID HEART_RATE_UUID_SERVICE = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
	private static final UUID HEART_RATE_MEASUREMENT_UUID_CHAR = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
	private final static UUID BATTERY_UUID_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb"); // battery
	private final static UUID BATTERY_LEVEL_UUID_CHAR = UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb"); // battery

	private final MutableLiveData<Integer> heartRateState = new MutableLiveData<>();
	private final MutableLiveData<Integer> batteryState = new MutableLiveData<>();
	private BluetoothGattCharacteristic heartRateCharacteristic, batteryLevelCharacteristic;
	private LogSession logSession;

	public BlinkyManager(@NonNull final Context context) {
		super(context);
	}

	public final LiveData<Integer> getHeartRate() {
		return heartRateState;
	}

	public final LiveData<Integer> getBatteryLevel() { return batteryState; }

	@NonNull
	@Override
	protected BleManagerGattCallback getGattCallback() {
		return new HeartRateManagerGattCallback();
	}

	/**
	 * Sets the log session to be used for low level logging.
	 * @param session the session, or null, if nRF Logger is not installed.
	 */
	public void setLogger(@Nullable final LogSession session) {
		logSession = session;
	}

	@Override
	public void log(final int priority, @NonNull final String message) {
		// The priority is a Log.X constant, while the Logger accepts it's log levels.
		Logger.log(logSession, LogContract.Log.Level.fromPriority(priority), message);
	}

	@Override
	protected boolean shouldClearCacheWhenDisconnected() { return heartRateCharacteristic == null; }

	// in no.nordicsemi.android.ble.common.callback.hr
	private final HeartRateMeasurementDataCallback HeartRateCallback = new HeartRateMeasurementDataCallback() {
		@Override
		public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
			super.onDataReceived(device, data);
		}

		@Override
		public void onHeartRateMeasurementReceived(@NonNull final BluetoothDevice device,
												   @IntRange(from = 0) final int heartRate,
												   @Nullable final Boolean contactDetected,
												   @Nullable @IntRange(from = 0) final Integer energyExpanded,
												   @Nullable final List<Integer> rrIntervals) {
			heartRateState.setValue(heartRate);
		}
	};

	// no.nordicsemi.android.ble.common.callback.battery
	private final DataReceivedCallback batteryLevelCallback = new BatteryLevelDataCallback() {
		@Override
		public void onBatteryLevelChanged(@NonNull final BluetoothDevice device,
										  @IntRange(from = 0, to = 100) final int batteryLevel) {
			batteryState.setValue(batteryLevel);
		}

		@Override
		public void onInvalidDataReceived(@NonNull final BluetoothDevice device, final @NonNull Data data) {
			super.onInvalidDataReceived(device, data);
		}
	};

	/**
	 * BluetoothGatt callbacks object.
	 */
	//TODO - having battery and heart rate in this class does not seem like best practice.
	// what is the correct implimentation? Not sure how to split in two classes in gattCallback()
	// only called once and expects one obj returned
	private class HeartRateManagerGattCallback extends BleManagerGattCallback {
		@Override
		protected void initialize() {
			// Heart rate
			setNotificationCallback(heartRateCharacteristic).with(HeartRateCallback);
			enableNotifications(heartRateCharacteristic).enqueue();

			// Battery
			readCharacteristic(batteryLevelCharacteristic).with(batteryLevelCallback).enqueue();
			setNotificationCallback(batteryLevelCharacteristic).with(batteryLevelCallback);
			enableNotifications(batteryLevelCharacteristic).enqueue();

		}

		@Override
		protected boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(HEART_RATE_UUID_SERVICE);
			if (service != null) {
				heartRateCharacteristic = service.getCharacteristic(HEART_RATE_MEASUREMENT_UUID_CHAR);
			}
			return heartRateCharacteristic != null;
		}

		@Override
		protected boolean isOptionalServiceSupported(@NonNull final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(BATTERY_UUID_SERVICE);
			if (service != null) {
				batteryLevelCharacteristic = service.getCharacteristic(BATTERY_LEVEL_UUID_CHAR);
			}
			return batteryLevelCharacteristic != null;
		}

		@Override
		protected void onDeviceDisconnected() {
			heartRateCharacteristic = null;
			batteryLevelCharacteristic = null;
		}
	}
}
