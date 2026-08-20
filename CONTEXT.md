# Context: tnc1-android-config

Mobilinkd TNC1 Android Configuration app.

## What this project is

An Android app that configures Mobilinkd TNC devices (TNC1, TNC2, TNC3) over Bluetooth SPP/RFCOMM. Users adjust audio input/output levels, DSP settings, KISS parameters, and power settings via a Bluetooth serial connection to the TNC.

## Tech stack

- Language: Java
- Build: Gradle with Android Gradle Plugin 8.13.1
- Compile SDK: 36
- Min SDK: 21 (inferred from project structure)
- Target SDK: 36
- Bluetooth: Classic Bluetooth SPP (RFCOMM), not BLE

## Domain glossary

| Term | Meaning |
|------|---------|
| TNC | Terminal Node Controller -- a modem that encodes/decodes AX.25 packet radio frames |
| SPP | Serial Port Profile -- Bluetooth protocol emulating a serial port |
| RFCOMM | Radio Frequency Communication -- the transport layer under SPP |
| KISS | Keep It Simple, Stupid -- a framing protocol for sending AX.25 frames over serial |
| AX.25 | Amateur radio link-layer protocol for packet transmission |
| HDLC | High-Level Data Link Control -- framing used by AX.25 |
| DSP | Digital Signal Processing -- audio filtering and modulation/demodulation |
| Intel HEX | Text-based binary encoding format for firmware files |
| AVR109 | Atmel bootloader protocol for firmware updates |

## Architecture

- `mobilinkdTNC/` -- Main app module (Activities, Fragments, Service)
- `library/` -- `net.simonvt.numberpicker` -- a vendored number picker widget
- `BluetoothTncService` -- Background service managing the Bluetooth RFCOMM connection
- `TncConfig` -- Main activity / configuration state container
- `FirmwareUpdateActivity` -- Firmware flashing via Intel HEX over the same Bluetooth connection

## Key flows

1. **Discovery** -- `DeviceListActivity` scans for paired Bluetooth devices
2. **Connection** -- `BluetoothTncService` opens an RFCOMM socket to the selected device
3. **Configuration** -- Fragments (`AudioInputFragment`, `AudioOutputFragment`, `KissFragment`, `ModemFragment`, `PowerFragment`) send/receive config over the open connection
4. **Firmware update** -- `FirmwareUpdateActivity` streams Intel HEX records to the TNC bootloader

## What to know before editing

- This is a **legacy Android app** from the early 2010s. It uses Fragments, `AsyncTask` (deprecated), and direct BluetoothAdapter APIs. Target platform is Android 5.0+.
- Bluetooth permissions vary by Android version: BLUETOOTH (classic) for pre-Android 12, BLUETOOTH_CONNECT for 12+.
- The app talks to the TNC in a custom binary protocol layered on top of raw serial bytes. The `HdlcDecoder` handles framing.
- The `library/` module is a vendored copy of `net.simonvt.numberpicker` -- don't refactor it unless replacing the UI component entirely.

## Current state

- Published on Google Play Store (legacy; planned for EOL once the ADR-001 unified app ships)
- Supports TNC1, TNC2, TNC3 (firmware flashing only needed for TNC1/TNC2)
- Firmware programming has moved to the standalone **Mobilinkd AVR TNC Programmer** app (`com.mobilinkd.avr.programmer`, live on Play per ADR-002) -- this app's `FirmwareUpdateActivity`/`Avr109` are superseded but not yet removed from the tree
- Needs modernization: target SDK bumps, permission model updates, and general cleanup; the configuration-only path is being replaced by ADR-001
