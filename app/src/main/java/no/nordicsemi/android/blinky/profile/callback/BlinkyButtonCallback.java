package no.nordicsemi.android.blinky.profile.callback;

import android.bluetooth.BluetoothDevice;

public interface BlinkyButtonCallback {

    /**
     * Called when a button was pressed or released on device.
     *
     * @param device the target device.
     * @param pressed true if the button was pressed, false if released.
     */
    void onButtonStateChanged(final BluetoothDevice device, final boolean pressed);
}
