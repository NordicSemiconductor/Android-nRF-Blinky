package no.nordicsemi.android.blinky.profile.data;

import no.nordicsemi.android.ble.data.Data;

public final class BlinkyLED {
    private static final byte STATE_OFF = 0x00;
    private static final byte STATE_ON = 0x01;

    public static Data turnOn() {
        return Data.opCode(STATE_ON);
    }

    public static Data turnOff() {
        return Data.opCode(STATE_OFF);
    }
}
