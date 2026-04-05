# SurvLink

Android app for receiving measurement data from total stations via Bluetooth. Built for surveyors who need a simple, reliable way to get data off their instruments and onto their phones.

> Latest release: `v1.4.0` — **GeoCOM Protocol Support for Leica Captivate Instruments**

## What it does

Connect your phone to a total station over classic Bluetooth, then:

**Real-time collection** — press "Start Receiving" on the phone, take measurements on the instrument, watch data appear live in the preview list.

**GeoCOM protocol support (NEW in v1.4.0)** — Leica Captivate instruments (TS60/TS16/TS50/MS60) now support bidirectional communication with:
- **Continuous polling mode**: Auto-measure every 2 seconds with "Start Auto" button
- **Single-shot mode**: Trigger individual measurements with "Measure Once" button
- **Real-time angle display**: View horizontal/vertical angles and slope distance instantly
- **Multi-unit export**: CSV export includes angles in radians, degrees, and gon

**Batch file import** — use the Data page to enter the model-aware import flow. TS09 keeps the current whole-file import path. TS60 now supports both legacy import and live GeoCOM measurement.

**Export & share** — export collected records as CSV or TXT, share via any Android app, or save directly to the Downloads folder.

## Supported instruments

| Brand | Models | Firmware | Protocol | Bluetooth |
|-------|--------|----------|----------|-----------|
| Leica | TS02, TS06, TS07, TS09, TS13 | FlexLine | GSI-Online (passive) | SPP ~15m |
| Leica | TS16, TS50, TS60 | Captivate | **GeoCOM (bidirectional)** ✨ | SPP + optional RH16 400m |
| Leica | MS60 MultiStation | Captivate | **GeoCOM (bidirectional)** ✨ | SPP + optional RH16 400m |
| Leica | iCON iCR80 | iCON | GSI-Online (passive) | SPP + CCD6 400m |
| Sokkia | SX-103/105/113, CX-101/105, iM-52/105, iX-1003 | — | Passive stream | SPP |
| Topcon | ES-105/120, MS-05AX, MS1AX, GT-1200 | — | Passive stream | SPP |
| South | NTS-362R, 552R, NTS-662R, R1-062 | — | Passive stream | SPP |
| CHC | HTS-221/321/661 | — | Passive stream | SPP |
| Hi-Target | ZTS-121/221, iTrack-5, iAngle X3 | — | Passive stream | SPP |

**Protocol Notes:**
- **GSI-Online (FlexLine)**: Passive push-based protocol, instrument sends data automatically
- **GeoCOM (Captivate)**: Bidirectional RPC protocol, app sends commands to trigger measurements
- **Passive stream**: Generic line-delimited or whitespace-token parsing

All instruments communicate over classic Bluetooth Serial Port Profile (SPP).

## Current support notes

- **Live receive:** 
  - **FlexLine instruments**: Passive GSI-Online protocol, instrument pushes data automatically
  - **Captivate instruments (NEW)**: Active GeoCOM protocol with two modes:
    - **Continuous polling**: "Start Auto" triggers measurements every 2 seconds
    - **Single-shot**: "Measure Once" triggers one measurement on demand
- **Real-time display**: Preview rows show measurement data with selectable/copyable text
  - **Captivate instruments**: Display includes horizontal angle, vertical angle, and slope distance in real-time
- **TS09 batch import:** Supported through the in-app import flow
- **TS60 live measurement (NEW):** Full GeoCOM protocol support with bidirectional communication
- **UI layout:** Two-page bottom-navigation (Bluetooth setup + Data actions)

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

### For FlexLine Instruments (TS02-TS13)
1. Open the app and go to the `蓝牙` page
2. Select your instrument brand and model
3. Turn on Bluetooth, pair with your total station from the nearby devices section if needed
4. Select the paired device, tap `连接`
5. Go to the `数据` page and tap `开始接收`
6. Take measurements on the instrument — data appears automatically in real-time
7. Use `导出并分享` to export as CSV/TXT

### For Captivate Instruments (TS60/TS16/TS50/MS60) — NEW in v1.4.0
1. Open the app and go to the `蓝牙` page
2. Select your Captivate instrument (e.g., TS60)
3. Turn on Bluetooth, pair with your total station
4. Select the paired device, tap `连接`
5. Go to the `数据` page — you'll see GeoCOM control panel:
   - **Start Auto**: Continuous polling mode (measurements every 2 seconds)
   - **Measure Once**: Single-shot measurement on demand
   - **Stop**: Stop continuous polling
6. Real-time display shows:
   - Horizontal angle (Hz) in degrees
   - Vertical angle (V) in degrees
   - Slope distance in meters
7. Use `导出并分享` to export — CSV includes angles in radians, degrees, and gon

### Batch Import (All Models)
1. Go to the `数据` page
2. Use the model-aware import action shown for your selected instrument
3. Use `分享文件` / `保存到本地` for imported-file artifacts

## Tech stack

- Kotlin + Jetpack Compose (two-page bottom-navigation UI)
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
│   ├── export/          # CSV/TXT export writers (including GeoCOM format)
│   ├── import_/         # raw file reception + format detection
│   ├── instrument/      # brand/model catalog
│   ├── protocol/        # protocol abstraction layer (NEW in v1.4.0)
│   │   ├── ProtocolHandler.kt           # protocol interface
│   │   ├── PassiveStreamProtocolHandler.kt  # GSI-Online/passive protocols
│   │   ├── GeoComProtocolHandler.kt     # GeoCOM bidirectional protocol
│   │   ├── DefaultProtocolHandlerFactory.kt # protocol selection
│   │   └── geocom/                      # GeoCOM implementation
│   │       ├── GeoComClient.kt          # RPC client with Mutex
│   │       ├── GeoComCommand.kt         # command builders
│   │       ├── GeoComResponse.kt        # response parser
│   │       └── GeoComMeasurement.kt     # measurement data + angle conversions
│   ├── share/           # Android share intent + Downloads saver
│   └── storage/         # Room database (sessions, records)
├── domain/model/        # Session, MeasurementRecord (extended with GeoCOM fields), InstrumentModel
├── ui/collector/        # CollectorScreen, ViewModel, UiState, Route
│   ├── GeoComControlPanel.kt    # GeoCOM UI controls (NEW)
│   └── MeasurementDisplay.kt    # Real-time angle display (NEW)
└── MainActivity.kt

src/appinventor/         # legacy MIT App Inventor source (reference only)
```

## Architecture

Single-activity, two-page MVVM with protocol abstraction:

- `CollectorViewModel` manages the state machine: connection lifecycle, live receive, import guidance, session persistence, export pipeline
- `CollectorScreen` renders the Bluetooth page and Data page from `CollectorUiState`
- `CollectorRoute` wires dependencies (Bluetooth controller, Room DB, export manager, protocol factory)
- **Protocol abstraction layer (NEW in v1.4.0)**:
  - `ProtocolHandler` interface: unified API for passive and bidirectional protocols
  - `PassiveStreamProtocolHandler`: wraps existing GSI-Online logic for FlexLine instruments
  - `GeoComProtocolHandler`: implements GeoCOM RPC protocol for Captivate instruments
  - `DefaultProtocolHandlerFactory`: selects protocol based on `firmwareFamily` field
- Two independent receive paths:
  - **Live mode (passive)**: `blockingReadBytes()` → `TextStreamRecordParser` → `MeasurementRecord` → Room DB
  - **Live mode (GeoCOM)**: `GeoComClient.sendCommand()` → `GeoComResponse.parse()` → `GeoComMeasurement` → `MeasurementRecord` → Room DB
  - **Import mode**: `rawFileReceiveLoop()` → `ByteArrayOutputStream` → file with auto-detected format

**GeoCOM Protocol Implementation:**
- `GeoComClient`: Mutex-protected command serialization with bounded timeout (5s max)
- `GeoComCommand`: Sealed class for RPC commands (GetSimpleMeasurement, GetCoordinate)
- `GeoComResponse`: Parser with RC_COM validation (communication status code)
- `GeoComMeasurement`: Data class with angle conversions (radians → degrees/gon)
- Polling interval: 2 seconds (configurable)

Bluetooth disconnect detection uses two redundant mechanisms:
1. `ACTION_ACL_DISCONNECTED` broadcast receiver
2. `IOException` catch in the blocking read loop

## Version history

| Version | Date | Changes |
|---------|------|---------|
| v1.4.0 | 2026-04-05 | **GeoCOM Protocol Support** — Full bidirectional communication for Leica Captivate instruments (TS60/TS16/TS50/MS60): continuous polling mode, single-shot measurement, real-time angle display (Hz/V/distance), multi-unit CSV export (rad/deg/gon), protocol abstraction layer, Room schema migration, 80%+ test coverage |
| v1.3.3 | 2026-04-03 | TS60 profile now switches both live and export actions to Captivate-specific labels/protocol summary |
| v1.3.2 | 2026-04-03 | Unlock instrument selection after disconnect/restart, auto-clear current session on actual selection change, keep imported files independent |
| v1.3.1 | 2026-04-03 | TS60 data page now exposes active receive/export action label, protocol summary, cleaner data-page controls, follow-up reliability fixes |
| v1.3.0 | 2026-04-03 | Split Bluetooth/Data pages, persistent import file panel, selection/session locking fix, TS60 import guidance branch |
| v1.2.0 | 2026-04-01 | Device selection unlock, raw file import, text copy, save to local, catalog update |
| v1.1.0 | 2026-03-31 | Bluetooth receive fix, disconnect detection, batch import mode |
| v1.0.0 | 2026-03-31 | Initial native Android release (Task 1-8 complete) |

## License

MIT License. See [LICENSE](LICENSE) for details.

Copyright (c) 2026 unf0rgettable
