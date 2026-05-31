# ADR-002: Standalone TNC Firmware Programmer App

## Status

Proposed

## Context

The legacy `tnc1-android-config` app embeds firmware programming code (`FirmwareUpdateActivity`, `Avr109` bootloader protocol, Intel HEX parser) inside the same APK that provides TNC configuration. This creates several problems:

1. **Architecture pollution**: The firmware code manages its own Bluetooth RFCOMM/SPP connection, duplicating transport logic and creating brittle state management.
2. **User confusion**: Users who only want to configure their TNC must install and navigate past firmware flashing screens.
3. **Play Store friction**: A unified app that "does everything" is harder to maintain, test, and get approved than focused tools.

TNC1 and TNC2 devices use the **AVR109 bootloader protocol** over **RFCOMM/SPP only**. They do not support BLE for firmware flashing. TNC3/TNC4 firmware updates are performed via USB, never BLE or this app.

Web Bluetooth cannot support RFCOMM/SPP, so a web-based firmware flasher is impossible. A native Android app is required.

## Decision

We will extract the firmware programming code from `tnc1-android-config` into a **standalone Android application** with its own Play Store listing.

### App Identity

| Attribute | Value |
|-----------|-------|
| **App Name** | Mobilinkd AVR TNC Programmer |
| **Package Name** | `com.mobilinkd.avr.programmer` |
| **Language** | Kotlin |
| **Min SDK** | 26 (matching BLEConfig baseline) |
| **Target SDK** | 34 (matching BLEConfig baseline) |

### Transport

- **RFCOMM/SPP only** -- no BLE support
- Inline connection thread pattern (no bound service abstraction needed; the app is single-purpose)

### Firmware Source

- Primary: Downloaded from the **Mobilinkd downloads site** (URL TBD -- see Open Questions)
- Secondary: **User-uploaded firmware files** (local filesystem, `.hex` format)
- No cryptographic signature verification required

### Offline Capability

- Downloaded firmware files are **cached locally**
- Cached firmware can be flashed without network connectivity
- User can always flash a locally-stored `.hex` file

### Release Order

**This app MUST be released BEFORE the unified TNC config app update** (ADR-001). Releasing the config app first would strip firmware programming capability from TNC1/TNC2 users before a replacement exists.

### Announcement Channels

- Mobilinkd support groups
- X/Twitter
- Mastodon
- Bluesky
- Reddit
- Facebook
- Website (main + support section)

## Consequences

### Positive

- Clean separation of concerns: one app configures, one app flashes
- Firmware UI can be purpose-built (progress bars, verification steps, recovery flows)
- Removes ~600 lines of firmware-specific code from the config app
- Easier to test bootloader protocol changes in isolation

### Negative

- Additional app to maintain, publish, and support
- Users must install two apps if they own both old and new TNCs
- Risk of orphaning TNC1/TNC2 users if release timing is wrong

## Open Questions

| # | Question | Status | Notes |
|---|----------|--------|-------|
| 1 | App name | **Resolved** | "Mobilinkd AVR TNC Programmer" |
| 2 | Package name | **Resolved** | `com.mobilinkd.avr.programmer` |
| 3 | Firmware download mechanism | **In discussion** | JSON manifest from mobilinkd.com; 4 models; user must select model; in-app download preferred |
| 4 | Announcement plan | **Resolved** | Support groups, X/Twitter, Mastodon, Bluesky, Reddit, Facebook, website, support section |

## Related

- ADR-001: Unified TNC Config App (BLE + Classic)
- `tnc1-android-config/mobilinkdTNC/src/main/java/com/mobilinkd/tncconfig/FirmwareUpdateActivity.java` -- source of extracted code
- `tnc1-android-config/mobilinkdTNC/src/main/java/com/mobilinkd/tncconfig/Avr109.java` -- bootloader protocol
- `tnc1-android-config/mobilinkdTNC/src/main/java/com/mobilinkd/tncconfig/IntelHexRecord.java` -- HEX parser
