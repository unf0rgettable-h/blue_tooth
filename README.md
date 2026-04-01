# SurvLink

Android app for receiving measurement data from total stations via Bluetooth. Built for surveyors who need a simple, reliable way to get data off their instruments and onto their phones.

> [Latest Release (v1.2.0)](https://github.com/unf0rgettable-h/blue_tooth/releases/tag/v1.2.0) — download the APK directly

## What it does

Connect your phone to a total station over classic Bluetooth, then:

**Real-time collection** — press "Start Receiving" on the phone, take measurements on the instrument, watch data appear live in the preview list.

**Batch file import** — press "Import Stored Data", trigger an export on the instrument, the phone receives the complete file and auto-detects the format (XML/LandXML, GSI-16, DXF, CSV, TXT). Transmission completes automatically after 3 seconds of silence.

**Export & share** — export collected records as CSV or TXT, share via any Android app, or save directly to the Downloads folder.

## Supported instruments

| Brand | Models | Firmware | Bluetooth |
|-------|--------|----------|-----------|
| Leica | TS02, TS06, TS07, TS09, TS13 | FlexLine | SPP ~15m |
| Leica | TS16, TS50, TS60 | Captivate | SPP + optional RH16 400m |
| Leica | MS60 MultiStation | Captivate | SPP + optional RH16 400m |
| Leica | iCON iCR80 | iCON | SPP + CCD6 400m |
| Sokkia | SX-103/105/113, CX-101/105, iM-52/105, iX-1003 | — | SPP |
| Topcon | ES-105/120, MS-05AX, MS1AX, GT-1200 | — | SPP |
| South | NTS-362R, 552R, NTS-662R, R1-062 | — | SPP |
| CHC | HTS-221/321/661 | — | SPP |
| Hi-Target | ZTS-121/221, iTrack-5, iAngle X3 | — | SPP |

All instruments communicate over classic Bluetooth Serial Port Profile (SPP). Leica instruments use line-delimited output (CR/LF); other brands use whitespace-token parsing.

## Quick start

### Install from release

Download `app-release-test-signed.apk` from the [Releases](https://github.com/unf0rgettable-h/blue_tooth/releases) page. Enable "Install from unknown sources" on your Android device.

### Build from source

Requirements: JDK 17, Android SDK (API 35)

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_SDK_ROOT=/path/to/android-sdk

./gradlew :app:assembleDebug          # debug APK
./gradlew :app:testDebugUnitTest      # run unit tests
```

## Usage

1. Open the app, select your instrument brand and model
2. Turn on Bluetooth, pair with your total station from the "Nearby Devices" section
3. Select the paired device, tap "Connect"
4. Choose your workflow:
   - **Live measurement**: tap "Start Receiving", take measurements on the instrument
   - **Batch import**: tap "Import Stored Data", trigger data export on the instrument
5. Preview data appears in real-time; long-press to select and copy text
6. Tap "Export & Share" to send as CSV/TXT, or "Save to Local" to save to Downloads

## Tech stack

- Kotlin + Jetpack Compose (single-screen UI)
- Room (session & record persistence)
- Kotlin Coroutines / StateFlow (reactive state machine)
- Classic Bluetooth SPP (`BluetoothSocket` blocking read)
- MediaStore (save to Downloads on API 29+)
- `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26`

## Project structure

```
app/src/main/java/com/unforgettable/bluetoothcollector/
├── data/
│   ├── bluetooth/       # connection, discovery, pairing, stream parsing
│   ├── export/          # CSV/TXT export writers
│   ├── import_/         # raw file reception + format detection
│   ├── instrument/      # brand/model catalog
│   ├── share/           # Android share intent + Downloads saver
│   └── storage/         # Room database (sessions, records)
├── domain/model/        # Session, MeasurementRecord, InstrumentModel, etc.
├── ui/collector/        # CollectorScreen, ViewModel, UiState, Route
└── MainActivity.kt

src/appinventor/         # legacy MIT App Inventor source (reference only)
```

## Architecture

Single-activity, single-screen MVVM:

- `CollectorViewModel` manages the state machine: connection lifecycle, receive loops, session persistence, export pipeline
- `CollectorScreen` renders the full UI from `CollectorUiState` via Compose
- `CollectorRoute` wires dependencies (Bluetooth controller, Room DB, export manager)
- Two independent receive paths:
  - **Live mode**: `blockingReadBytes()` → `TextStreamRecordParser` → `MeasurementRecord` → Room DB
  - **Import mode**: `rawFileReceiveLoop()` → `ByteArrayOutputStream` → file with auto-detected format

Bluetooth disconnect detection uses two redundant mechanisms:
1. `ACTION_ACL_DISCONNECTED` broadcast receiver
2. `IOException` catch in the blocking read loop

## Version history

| Version | Date | Changes |
|---------|------|---------|
| v1.2.0 | 2026-04-01 | Device selection unlock, raw file import, text copy, save to local, catalog update |
| v1.1.0 | 2026-03-31 | Bluetooth receive fix, disconnect detection, batch import mode |
| v1.0.0 | 2026-03-31 | Initial native Android release (Task 1-8 complete) |

## License

MIT License. See [LICENSE](LICENSE) for details.

Copyright (c) 2026 unf0rgettable
