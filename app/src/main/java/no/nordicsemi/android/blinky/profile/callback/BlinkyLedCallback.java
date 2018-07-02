package no.nordicsemi.android.blinky.profile.callback;

import android.bluetooth.BluetoothDevice;

public interface BlinkyLedCallback {

    /**
     * Called when the data has been sent to the connected device.
     *
     * @param device the target device.
     * @param on true when LED was enabled, false when disabled.
     */
    void onLedStateChanged(final BluetoothDevice device, final boolean on);
}
