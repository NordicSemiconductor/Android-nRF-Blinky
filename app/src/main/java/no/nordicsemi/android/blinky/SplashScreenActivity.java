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

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.core.splashscreen.SplashScreen;

/**
 * This is a Splash Screen activity.
 * <p>
 * It is using AndroidX SplashScreen class to emulate the native (API 31+) splashscreen on
 * Android 5 - 11. The compat library does not show animated vector drawables, so a static drawable
 * is used instead.
 * @see <a href="https://developer.android.com/reference/kotlin/androidx/core/splashscreen/SplashScreen">androidx.core.splashscreen.SplashScreen</a>
 */
public class SplashScreenActivity extends Activity {
	// This flag is false when the app is first started (cold start).
	// In this case, the animation will be fully shown (1 sec).
	// Subsequent launches will display it only briefly.
	// It is only used on API 31+
	public static boolean coldStart = true;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Skip splash screen in debug. That allows running the app from Android Studio.
		if (BuildConfig.DEBUG) {
			launchMainActivity();
			SplashScreen.installSplashScreen(this);
			return;
		}

		// The compat SplashScreen library requires API 21.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			final SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
			splashScreen.setOnExitAnimationListener(provider -> launchMainActivity());

			// Animated Vector Drawable is only supported on API 31+.
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				if (coldStart) {
					coldStart = false;
					// Keep the splash screen on-screen for longer periods.
					// Handle the splash screen transition.
					final long then = System.currentTimeMillis();
					splashScreen.setKeepVisibleCondition(() -> {
						final long now = System.currentTimeMillis();
						return now < then + 1300;
					});
				}
			}
		} else {
			// On API 18-20 the activity is drawn with "android:windowBackground" set to
			// the 9-patch with an icon on light background, which looks like the splash screen.
			launchMainActivity();
		}
	}

	private void launchMainActivity() {
		final Intent intent = new Intent(this, ScannerActivity.class);
		startActivity(intent);
		finish();
	}
}
