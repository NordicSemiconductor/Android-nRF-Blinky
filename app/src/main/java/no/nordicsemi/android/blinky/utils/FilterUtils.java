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

package no.nordicsemi.android.blinky.utils;

import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import no.nordicsemi.android.support.v18.scanner.ScanRecord;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class FilterUtils {
    private static final ParcelUuid EDDYSTONE_UUID
            = ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805f9b34fb");

    private static final int COMPANY_ID_MICROSOFT = 0x0006;
    private static final int COMPANY_ID_APPLE = 0x004C;
    private static final int COMPANY_ID_NORDIC_SEMI = 0x0059;

    @SuppressWarnings("RedundantIfStatement")
    public static boolean isBeacon(@NonNull final ScanResult result) {
        if (result.getScanRecord() != null) {
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

    public static boolean isAirDrop(@NonNull final ScanResult result) {
        if (result.getScanRecord() != null) {
            final ScanRecord record = result.getScanRecord();

            // iPhones and iMacs advertise with AirDrop packets
            final byte[] appleData = record.getManufacturerSpecificData(COMPANY_ID_APPLE);
            return appleData != null && appleData.length > 1 && appleData[0] == 0x10;
        }
        return false;
    }
}
