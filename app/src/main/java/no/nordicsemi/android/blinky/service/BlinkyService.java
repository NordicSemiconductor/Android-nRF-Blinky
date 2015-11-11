/*
 * Copyright (c) 2015, Nordic Semiconductor
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

package no.nordicsemi.android.blinky.service;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import no.nordicsemi.android.blinky.profile.BleManager;
import no.nordicsemi.android.blinky.profile.BleProfileService;

public class BlinkyService extends BleProfileService implements BlinkyManagerCallbacks {
	private static final String TAG = "BlinkyService";

	public static final String BROADCAST_LED_STATE_CHANGED = "no.nordicsemi.android.nrfblinky.BROADCAST_LED_STATE_CHANGED";
	public static final String BROADCAST_BUTTON_STATE_CHANGED = "no.nordicsemi.android.nrfblinky.BROADCAST_BUTTON_STATE_CHANGED";
	public static final String EXTRA_DATA = "no.nordicsemi.android.nrfblinky.EXTRA_DATA";

	private BlinkyManager mManager;

	private final BlinkyBinder mBinder = new BlinkyBinder();

	public class BlinkyBinder extends BleProfileService.LocalBinder implements BlinkyInterface {
		private boolean mLEDState;
		private boolean mButtonState;

		@Override
		public void send(final boolean onOff) {
			mManager.send(mLEDState = onOff);
		}

		public boolean isOn() {
			return mLEDState;
		}

		public boolean isButtonPressed() {
			return mButtonState;
		}
	}

	@Override
	protected LocalBinder getBinder() {
		return mBinder;
	}

	@Override
	protected BleManager<BlinkyManagerCallbacks> initializeManager() {
		return mManager = new BlinkyManager(this);
	}

	@Override
	public void onDataReceived(final boolean state) {
		mBinder.mButtonState = state;

		final Intent broadcast = new Intent(BROADCAST_BUTTON_STATE_CHANGED);
		broadcast.putExtra(EXTRA_DATA, state);
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
	}

	@Override
	public void onDataSent(boolean state) {
		mBinder.mLEDState = state;

		final Intent broadcast = new Intent(BROADCAST_LED_STATE_CHANGED);
		broadcast.putExtra(EXTRA_DATA, state);
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
	}
}
