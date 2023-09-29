/*
 * Copyright (c) 2022, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.scanner.main.viewmodel

import android.os.ParcelUuid
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import no.nordicsemi.android.scanner.repository.DevicesScanFilter
import no.nordicsemi.android.scanner.repository.ScannerRepository
import no.nordicsemi.android.scanner.repository.ScanningState
import javax.inject.Inject

private const val FILTER_RSSI = -50 // [dBm]

@HiltViewModel
internal class ScannerViewModel @Inject constructor(
    private val scannerRepository: ScannerRepository,
) : ViewModel() {
    private var uuid: ParcelUuid? = null

    val filterConfig = MutableStateFlow(
        DevicesScanFilter(
            filterUuidRequired = true,
            filterNearbyOnly = false,
            filterWithNames = true
        )
    )

    val state = filterConfig
        .combine(scannerRepository.getScannerState()) { config, result ->
            when (result) {
                is ScanningState.DevicesDiscovered -> result.applyFilters(config)
                else -> result
            }
        }
        // This can't be observed in View Model Scope, as it can exist even when the
        // scanner is not visible. Scanner state stops scanning when it is not observed.
        // .stateIn(viewModelScope, SharingStarted.Lazily, ScanningState.Loading)

    private fun ScanningState.DevicesDiscovered.applyFilters(config: DevicesScanFilter) =
        ScanningState.DevicesDiscovered(devices
            .filter {
                uuid == null ||
                config.filterUuidRequired == false ||
                it.scanResult?.scanRecord?.serviceUuids?.contains(uuid) == true
            }
            .filter { !config.filterNearbyOnly || it.highestRssi >= FILTER_RSSI }
            .filter { !config.filterWithNames || it.hadName }
        )

    fun setFilterUuid(uuid: ParcelUuid?) {
        this.uuid = uuid
        if (uuid == null) {
            filterConfig.value = filterConfig.value.copy(filterUuidRequired = null)
        }
    }

    fun setFilter(config: DevicesScanFilter) {
        this.filterConfig.value = config
    }

    fun refresh() {
        scannerRepository.clear()
    }

    override fun onCleared() {
        super.onCleared()
        scannerRepository.clear()
    }
}