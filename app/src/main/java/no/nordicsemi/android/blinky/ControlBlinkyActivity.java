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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import no.nordicsemi.android.blinky.profile.BleProfileService;
import no.nordicsemi.android.blinky.service.BlinkyService;
import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.Logger;

public class ControlBlinkyActivity extends AppCompatActivity {
	private BlinkyService.BlinkyBinder mBlinkyDevice;
	private Button mActionOnOff, mActionConnect;
	private ImageView mImageBulb;
	private View mParentView;
	private View mBackgroundView;
	private ILogSession mLogSession;

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(final ComponentName name, final IBinder service) {
			mBlinkyDevice = (BlinkyService.BlinkyBinder) service;

			if (mBlinkyDevice.isConnected()) {
				mActionConnect.setText(getString(R.string.action_disconnect));

				if (mBlinkyDevice.isOn()) {
					mImageBulb.setImageDrawable(ContextCompat.getDrawable(ControlBlinkyActivity.this, R.drawable.bulb_on));
					mActionOnOff.setText(getString(R.string.turn_off));
				} else {
					mImageBulb.setImageDrawable(ContextCompat.getDrawable(ControlBlinkyActivity.this, R.drawable.bulb_off));
					mActionOnOff.setText(getString(R.string.turn_on));
				}

				if (mBlinkyDevice.isButtonPressed()) {
					mBackgroundView.setVisibility(View.VISIBLE);
				} else {
					mBackgroundView.setVisibility(View.INVISIBLE);
				}
			} else {
				mActionConnect.setText(getString(R.string.action_connect));
			}
		}

		@Override
		public void onServiceDisconnected(final ComponentName name) {
			mBlinkyDevice = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_control_device);

		final Intent i = getIntent();
		final String deviceName = i.getStringExtra(BlinkyService.EXTRA_DEVICE_NAME);
		final String deviceAddress = i.getStringExtra(BlinkyService.EXTRA_DEVICE_ADDRESS);

		final Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setTitle(deviceName);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mActionOnOff = findViewById(R.id.button_blinky);
		mActionConnect = findViewById(R.id.action_connect);
		mImageBulb = findViewById(R.id.img_bulb);
		mBackgroundView = findViewById(R.id.background_view);
		mParentView = findViewById(R.id.relative_layout_control);

		mActionOnOff.setOnClickListener(v -> {
			if (mBlinkyDevice != null && mBlinkyDevice.isConnected()) {
				if (mActionOnOff.getText().equals(getString(R.string.turn_on))) {
					mBlinkyDevice.send(true);
				} else {
					mBlinkyDevice.send(false);
				}
			} else {
				showError(getString(R.string.please_connect));
			}
		});

		LocalBroadcastManager.getInstance(this).registerReceiver(mBlinkyUpdateReceiver, makeGattUpdateIntentFilter());
		mLogSession = Logger.newSession(getApplicationContext(), null, deviceAddress, deviceName);

		// The device may not be in the range but the service will try to connect to it if it reach it
		Logger.d(mLogSession, "Creating service...");
		final Intent intent = new Intent(this, BlinkyService.class);
		intent.putExtra(BlinkyService.EXTRA_DEVICE_ADDRESS, deviceAddress);
		intent.putExtra(BleProfileService.EXTRA_DEVICE_NAME, deviceName);
		if (mLogSession != null)
			intent.putExtra(BleProfileService.EXTRA_LOG_URI, mLogSession.getSessionUri());
		startService(intent);
		Logger.d(mLogSession, "Binding to the service...");
		bindService(intent, mServiceConnection, 0);

		mActionConnect.setOnClickListener(v -> {
			if (mBlinkyDevice != null && mBlinkyDevice.isConnected()) {
				mBlinkyDevice.disconnect();
			} else {
				startService(intent);
				bindService(intent, mServiceConnection, 0);
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
		}
		return false;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		if (mBlinkyDevice != null && mBlinkyDevice.isConnected())
			mBlinkyDevice.disconnect();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Logger.d(mLogSession, "Unbinding from the service...");
		unbindService(mServiceConnection);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mBlinkyUpdateReceiver);

		mServiceConnection = null;
		mBlinkyDevice = null;
		Logger.d(mLogSession, "Activity unbound from the service");
	}

	private BroadcastReceiver mBlinkyUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			switch (action) {
				case BlinkyService.BROADCAST_LED_STATE_CHANGED: {
					final boolean flag = intent.getBooleanExtra(BlinkyService.EXTRA_DATA, false);
					if (flag) {
						mImageBulb.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.bulb_on));
						mActionOnOff.setText(getString(R.string.turn_off));
					} else {
						mImageBulb.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.bulb_off));
						mActionOnOff.setText(getString(R.string.turn_on));
					}
					break;
				}
				case BlinkyService.BROADCAST_BUTTON_STATE_CHANGED: {
					final boolean flag = intent.getBooleanExtra(BlinkyService.EXTRA_DATA, false);
					if (flag) {
						mBackgroundView.setVisibility(View.VISIBLE);
					} else {
						mBackgroundView.setVisibility(View.INVISIBLE);
					}
					break;
				}
				case BlinkyService.BROADCAST_CONNECTION_STATE: {
					final int value = intent.getIntExtra(BlinkyService.EXTRA_CONNECTION_STATE, BlinkyService.STATE_DISCONNECTED);
					switch (value) {
						case BleProfileService.STATE_CONNECTED:
							mActionConnect.setText(getString(R.string.action_disconnect));
							break;
						case BleProfileService.STATE_DISCONNECTED:
							mActionConnect.setText(getString(R.string.action_connect));
							mActionOnOff.setText(getString(R.string.turn_on));
							mImageBulb.setImageDrawable(ContextCompat.getDrawable(ControlBlinkyActivity.this, R.drawable.bulb_off));
							break;
					}
					break;
				}
				case BlinkyService.BROADCAST_ERROR: {
					final String message = intent.getStringExtra(BlinkyService.EXTRA_ERROR_MESSAGE);
					final int code = intent.getIntExtra(BlinkyService.EXTRA_ERROR_CODE, 0);
					showError(getString(R.string.error_msg, message, code));
					break;
				}
			}
		}
	};

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BlinkyService.BROADCAST_LED_STATE_CHANGED);
		intentFilter.addAction(BlinkyService.BROADCAST_BUTTON_STATE_CHANGED);
		intentFilter.addAction(BlinkyService.BROADCAST_CONNECTION_STATE);
		intentFilter.addAction(BlinkyService.BROADCAST_ERROR);
		return intentFilter;
	}

	private void showError(final String error) {
		Snackbar.make(mParentView, error, Snackbar.LENGTH_LONG).show();
	}
}
