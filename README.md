# nRF BLINKY

nRF Blinky is an application developed targeting an audience of developers who are new to 
Bluetooth Low Energy. This is a very simple application with two basic features to turn on LED 3 
on the nRF DK and to receive the Button 1 press event from a nRF DK on the nRF Blinky Application.
It demonstrates how to the **BleManager** class from 
[Android BLE Library](https://github.com/NordicSemiconductor/Android-BLE-Library/) 
library can be used from View Model 
(see [Architecture Components](https://developer.android.com/topic/libraries/architecture/index.html)).

## Nordic LED and Button Service

Service UUID: `00001523-1212-EFDE-1523-785FEABCD123`

A simplified proprietary service by Nordic Semiconductor, containing two characteristics one to 
control LED 3 and Button 1:

- First characteristic controls the LED state (On/Off).
  - UUID: **`00001525-1212-EFDE-1523-785FEABCD123`**
  - Value: **`1`** => LED On
  - Value: **`0`** => LED Off

- Second characteristic notifies central of the button state on change (Pressed/Released).
  - UUID: **`00001524-1212-EFDE-1523-785FEABCD123`**
  - Value: **`1`** => Button Pressed
  - Value: **`0`** => Button Released
  
For full specification, check out 
[documentation](https://infocenter.nordicsemi.com/topic/sdk_nrf5_v16.0.0/ble_sdk_app_blinky.html?cp=7_1_4_2_2_3).

## Requirements

* This application depends on [Android BLE Library](https://github.com/NordicSemiconductor/Android-BLE-Library/) version 2.2.
* Android 4.3 or newer is required.
* nRF5 DK is required in order to test the BLE Blinky service.

## Installation and usage

Prepare your Development kit.
  - Plug in the Development Kit to your computer via USB.
  - Power On the Development Kit.
  - The Development Kit will now appear as a Mass storage device.
  - Drag (or copy/paste) the appropriate HEX file onto that new device.
  - The Development Kit LEDs will flash and it will disconnect and reconnect.
  - The Development Kit is now ready and flashed with the nRF Blinky example firmware.

For your convenience, we have bundled two firmwares in this project under the 
[Firmwares](https://github.com/NordicSemiconductor/Android-nRF-Blinky/tree/master/Firmwares) 
directory.

To get the latest firmwares and check the source code, you may go directly to our 
[Developers website](http://developer.nordicsemi.com/nRF5_SDK/) 
and download the SDK version you need. Then, find the source code and hex files in the 
directory `/examples/ble_peripheral/ble_app_blinky/`.

More information about the nRF Blinky example firmware can be found in the 
[documentation](https://www.nordicsemi.com/DocLib/Content/SDK_Doc/nRF5_SDK/v15-2-0/ble_sdk_app_blinky).

## Note

In order to scan for Bluetooth LE device the Location permission must be granted and, on some phones, 
the Location must be enabled. This app will not use the location information in any way.