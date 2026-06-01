# ADR-001: Add RFCOMM/SPP Transport to BLEConfig for TNC1/TNC2 Support

## Status

Proposed

## Context

Mobilinkd currently maintains two Android configuration app repositories:

- **BLEConfig** (Kotlin, BLE GATT) -- configures TNC3/TNC4 via Bluetooth Low Energy. Modern architecture: Jetpack Navigation, ViewModel, Kotlin coroutines-friendly patterns. Actively developed.
- **tnc1-android-config** (Java, RFCOMM/SPP) -- configures TNC1/TNC2 via Bluetooth Classic serial port. Legacy architecture: monolithic Activities, Handler-based UI updates, deprecated API patterns. Currently published on Google Play as `com.mobilinkd.tncconfig`.

Maintaining two config apps for overlapping TNC families is unsustainable. The goal is to **update the existing Play Store app** (`com.mobilinkd.tncconfig`) with a unified codebase that supports both BLE and Classic transport.

### The Android Dual-Mode Pairing Problem

TNC3/TNC4 share a single MAC address for both BLE and Bluetooth Classic. Android's Bluetooth stack does not handle this well: once a device is paired in one mode, Android often fails to discover or connect to it in the other mode. This is a known, long-standing Android limitation with single-MAC dual-mode devices.

**Consequence:** The app cannot scan BLE and Classic concurrently and merge results. The user must explicitly select the transport mode (BLE or Classic) before scanning, and the scan must filter for only that transport.

### Technical Compatibility

- The **KISS protocol layer** is already transport-agnostic. `TncInterface` writes to an `OutputStream` and `KissDecoder` reads raw bytes -- both work identically over BLE or RFCOMM.
- The **command byte definitions** are identical between both codebases.
- The **only transport-specific code** is `BluetoothLEService` (~815 lines of GATT plumbing).

## Decision

We will **refactor BLEConfig to support both BLE GATT and RFCOMM/SPP** via a single, mode-aware Bluetooth service abstraction. This unified codebase will be released as an update to the existing Play Store app (`com.mobilinkd.tncconfig`), replacing the legacy Java codebase for existing TNC1/TNC2 users while adding BLE support for TNC3/TNC4 users.

### Chosen Approach: Unified Service with Mode-Switch Internals

Rather than maintaining two parallel `Service` classes, we will:

1. **Abstract `BluetoothLEService`** into `BluetoothService` with a `TransportMode` enum:
   ```kotlin
   enum class TransportMode { BLE, CLASSIC }
   ```

2. **Keep the existing `InputStream`/`OutputStream` contract** that `TncInterface` already consumes. The service implementation swaps between:
   - **BLE mode:** Existing `BluetoothGatt` plumbing feeding `BLEInputStream`/`BLEOutputStream`
   - **Classic mode:** New `BluetoothSocket` (RFCOMM) plumbing feeding raw `socket.inputStream`/`socket.outputStream`

3. **The upper layers (`TncInterface`, `KissEncoder`, `KissDecoder`, `TncViewModel`, all Fragments) remain unchanged.**

4. **UX:** `SelectDeviceFragment` gains a **transport mode selector** (BLE vs. Classic) before scan. The chosen mode is passed into the service and persists for the session.

## Consequences

### Positive
- **One app, one codebase, one Play Store listing** for all TNC configuration.
- TNC1/TNC2 users get a modern, maintained app instead of legacy `tnc1-android-config`.
- Existing BLE GATT code is preserved; only the service class gets conditional internal logic.
- `TncInterface`, `KissEncoder`, `KissDecoder` require **zero changes** -- they were already designed for this.
- Testing is unified: same fragment-level tests, same ViewModel tests, same command tests regardless of transport.

### Negative / Trade-offs
- `BluetoothService` will contain two very different connection state machines internally (GATT callbacks vs. blocking `BluetoothSocket` threads). This is a classic "two halves" service but is still simpler than two full service lifecycles.
- Classic mode needs `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` permissions on Android 12+; BLE already needs these, so the permission surface does not expand.
- Classic pairing UI (PIN entry) may surface on connection for TNC1/TNC2, unlike BLE which handles bonding differently. We need to handle the `ACTION_PAIRING_REQUEST` broadcast for Classic mode.
- The dual-mode pairing bug means users who previously paired a TNC3 in BLE mode must explicitly unpair (or switch) if they want to use Classic mode. We should document this.

## Alternatives Considered

### Alternative A: Parallel `BluetoothSPPService`

Instead of a unified service, add a second `Service` that mirrors `BluetoothLEService` but uses `BluetoothSocket`. `MainActivity` binds to whichever the user selected.

**Rejected:** More lifecycle duplication (bind/unbind for two services), more memory footprint, harder to maintain two connection state machines. The existing BLE code already wraps transport into `InputStream`/`OutputStream` -- there is no architectural reason to keep the services separate.

### Alternative B: Keep Two Separate Apps

Continue maintaining BLEConfig for TNC3/TNC4 and tnc1-android-config for TNC1/TNC2.

**Rejected:** The legacy Java app has known NPEs (GitHub issues #6, #7), lacks modern Android features (targetSdk is stale), and the codebase is brittle. Dual maintenance is unsustainable for a small business. Replacing it with the modern app is the whole point.

### Alternative C: Full Rewrite in Shared C++ / NDK

Use a cross-platform BLE+SPP library (e.g., via NDK or a JNI bridge).

**Rejected:** Massive scope increase. No portability requirement -- this is Android-only. Would delay the unified app by months with no user-facing benefit.

## Related Work

- **ADR-002:** Standalone TNC Firmware Programmer App (separation of `FirmwareUpdateActivity` / `Avr109` from tnc1-android-config into a new Play Store listing)
- **Issue #3:** tnc1-android-config -- check newer firmware (will migrate to new firmware app)
- **Issue #6, #7:** tnc1-android-config NPEs (will be resolved by EOL'ing tnc1-android-config and using the unified app)

## Open Questions (not blockers -- assumptions embedded above)

1. **App name on Play Store:** Keep the existing `com.mobilinkd.tncconfig` listing. The in-app branding and store listing text should be updated to reflect BLE + Classic support.
2. **Repository rename:** Consider renaming the `BLEConfig` repository to `TNC-Config-Android` (or similar) to reflect the unified scope.
3. **Firmware app transport** (resolved): SPP-only. TNC3/TNC4 firmware updates are done via USB, never BLE.
4. **Play Store update sequence:** How do we manage the first release containing the new BLE transport? The legacy app never had BLE; existing TNC1/TNC2 users will see a new "BLE" mode in the transport selector on update. Is a changelog entry sufficient, or do we want a one-time onboarding flow?

## Implementation Sketch

### Phase 1: Abstract the Service
- Rename `BluetoothLEService` → `BluetoothService`
- Add `TransportMode` enum and `var transportMode: TransportMode` to service
- Extract BLE plumbing into inner/encapsulated `BleTransportManager`
- Add `ClassicTransportManager` using `BluetoothSocket` + `ConnectThread` + `ConnectedThread` pattern

### Phase 2: Add Mode Selection to `SelectDeviceFragment`
- UI: Toggle or segmented button: "BLE / Classic"
- BLE mode: use existing `BluetoothLeScanner` with Mobilinkd service UUID filter
- Classic mode: list `BluetoothAdapter.bondedDevices` + new discovery via `BluetoothAdapter.startDiscovery()`
- Pass selected mode + device to `ConnectingFragment` → `BluetoothService`

### Phase 3: Classic Connection Lifecycle
- `ConnectThread`: `device.createRfcommSocketToServiceRecord(SPP_UUID)` → `socket.connect()`
- `ConnectedThread`: read from `socket.inputStream`, write to `socket.outputStream`
- Wire `InputStream`/`OutputStream` directly to `TncInterface` (no custom wrapper needed, unlike BLE which needs GATT chunking)
- Handle pairing broadcasts for secured RFCOMM sockets

### Phase 4: Regression Test
- Verify BLE mode unchanged against TNC4
- Verify Classic mode connects to TNC1/TNC2
- Verify `KissEncoder`/`KissDecoder` work identically on both transports
- Verify all fragments populate correctly from `TncViewModel`
