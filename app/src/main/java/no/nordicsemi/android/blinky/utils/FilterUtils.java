/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.blinky.utils;

import android.os.ParcelUuid;

import no.nordicsemi.android.support.v18.scanner.ScanRecord;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class FilterUtils {
    private static final ParcelUuid EDDYSTONE_UUID
            = ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805f9b34fb");

    private static final int COMPANY_ID_MICROSOFT = 0x0006;
    private static final int COMPANY_ID_APPLE = 0x004C;
    private static final int COMPANY_ID_NORDIC_SEMI = 0x0059;

    @SuppressWarnings("RedundantIfStatement")
    public static boolean isBeacon(final ScanResult result) {
        if (result != null && result.getScanRecord() != null) {
            final ScanRecord record = result.getScanRecord();

            final byte[] appleData = record.getManufacturerSpecificData(COMPANY_ID_APPLE);
            if (appleData != null) {
                // iBeacons
                if (appleData.length == 23 && appleData[0] == 0x02 && appleData[1] == 0x15)
                    return true;
            }

            final byte[] nordicData = record.getManufacturerSpecificData(COMPANY_ID_NORDIC_SEMI);
            if (nordicData != null) {
                // Nordic Beacons
                if (nordicData.length == 23 && nordicData[0] == 0x02 && nordicData[1] == 0x15)
                    return true;
            }

            final byte[] microsoftData = record.getManufacturerSpecificData(COMPANY_ID_MICROSOFT);
            if (microsoftData != null) {
                // Microsoft Advertising Beacon
                if (microsoftData[0] == 0x01) // Scenario Type = Advertising Beacon
                    return true;
            }

            // Eddystone
            final byte[] eddystoneData = record.getServiceData(EDDYSTONE_UUID);
            if (eddystoneData != null)
                return true;
        }

        return false;
    }

    public static boolean isAirDrop(final ScanResult result) {
        if (result != null && result.getScanRecord() != null) {
            final ScanRecord record = result.getScanRecord();

            // iPhones and iMacs advertise with AirDrop packets
            final byte[] appleData = record.getManufacturerSpecificData(COMPANY_ID_APPLE);
            return appleData != null && appleData.length > 1 && appleData[0] == 0x10;
        }
        return false;
    }
}
