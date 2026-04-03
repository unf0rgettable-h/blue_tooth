# SurvLink

Android app for receiving measurement data from total stations via Bluetooth. Built for surveyors who need a simple, reliable way to get data off their instruments and onto their phones.

> Latest release target: `v1.3.3`

## What it does

Connect your phone to a total station over classic Bluetooth, then:

**Real-time collection** — press "Start Receiving" on the phone, take measurements on the instrument, watch data appear live in the preview list.

**Batch file import** — use the Data page to enter the model-aware import flow. TS09 keeps the current whole-file import path. TS60 currently routes to guidance/limitations instead of pretending the TS09 path is guaranteed to work.

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

## Current support notes

- **Live receive:** supported for the current collector workflow, with preview rows remaining selectable/copyable
- **TS09 batch import:** supported through the in-app import flow
- **TS60 batch import:** not treated as identical to TS09; the app now surfaces model-aware guidance instead of assuming the same ASCII/RS232 export path always works
- **UI layout:** the app now separates Bluetooth setup and Data actions into two bottom-navigation pages to reduce crowding on smaller phones

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

1. Open the app and go to the `蓝牙` page
2. Select your instrument brand and model
3. Turn on Bluetooth, pair with your total station from the nearby devices section if needed
4. Select the paired device, tap `连接`, and use `断开` from the same page when needed
5. Go to the `数据` page and choose your workflow:
   - **Live measurement**: tap `开始接收`, take measurements on the instrument
   - **Batch import**: use the model-aware import action shown for the selected instrument
6. Preview data appears in real time; preview text remains selectable/copyable
7. Use `导出并分享` for session records, or `分享文件` / `保存到本地` for imported-file artifacts

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

Single-activity, two-page MVVM:

- `CollectorViewModel` manages the state machine: connection lifecycle, live receive, import guidance, session persistence, export pipeline
- `CollectorScreen` renders the Bluetooth page and Data page from `CollectorUiState`
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
