# nRF Blinky for Android

nRF Blinky is an application targeting an audience of developers who are new to 
Bluetooth Low Energy. 

This is a very simple application with two basic features:
* turning on a LED 
* receiving a Button press and release events.

![Scanner](images/scanner.png) ![Blinky](images/blinky.png)

It demonstrates how the `BleManager` class from 
[Android BLE Library](https://github.com/NordicSemiconductor/Android-BLE-Library/) 
library can be used from a View Model 
(see [Architecture Components](https://developer.android.com/topic/libraries/architecture/index.html)).

## Nordic LED and Button Service (LBS)

Service UUID: `00001523-1212-EFDE-1523-785FEABCD123`

A simplified proprietary service by Nordic Semiconductor, containing two characteristics:

- LED state (On/Off).
  - UUID: **`00001525-1212-EFDE-1523-785FEABCD123`**
  - Properties: **Write** or **Write Without Response**
    - Value: **`0x01`** => LED On
    - Value: **`0x00`** => LED Off

- Button state (Pressed/Released).
  - UUID: **`00001524-1212-EFDE-1523-785FEABCD123`**
  - Properties: **Notify**
    - Value: **`0x01`** => Button Pressed
    - Value: **`0x00`** => Button Released
  
For documentation for nRF5 SDK, check out 
[this link](https://infocenter.nordicsemi.com/topic/sdk_nrf5_v17.1.0/ble_sdk_app_blinky.html?cp=8_1_4_2_2_3)
and for one based on nRF Connect SDK 
[this link](https://developer.nordicsemi.com/nRF_Connect_SDK/doc/latest/nrf/samples/bluetooth/peripheral_lbs/README.html).

## Requirements

* This application depends on [Android BLE Library](https://github.com/NordicSemiconductor/Android-BLE-Library/).
* Android 4.3 or newer is required.
* Any nRF5 DK or another device is required in order to test the BLE Blinky service. The service 
  can also be emulated using nRF Connect for Android, iOS or Desktop.

## Installation and usage

Program your device with LED Button sample from nRF5 SDK (blinky sample) or nRF Connect SDK (LBS sample).

The device should appear on the scanner screen after granting required permissions.

### Required permissions

On Android 6 - 11 nRF Blinky will ask for Location Permission and Location services. 
This permission is required on Android in order to obtain Bluetooth LE scan results. The app does not
use location in any way and has no Internet permission so can be used safely.

This permission is not required from Android 12 onwards, where new 
[Bluetooth permissions](https://developer.android.com/guide/topics/connectivity/bluetooth/permissions)
were introduced. The `BLUETOOTH_SCAN` permission can now be requested with 
`usesPermissionFlags="neverForLocation"` parameter, which excludes location related data from the
scan results, making requesting location not needed anymore.