# TS09 / TS60 Field Behavior Investigation

## Scope

This note captures the investigation baseline for SurvLink vNext before code changes begin. It records:

- field-reported behavior
- repository history findings
- protocol/documentation findings
- decision gates for TS60 batch import

## Field-Reported Facts

- TS09 batch import currently succeeds through the in-app `导入存储数据` flow.
- TS60 batch import has not succeeded through that same flow.
- On TS60, the observed instrument-side error is: unable to transfer data to RS232 device, with guidance to check cable, communication protocol, format file, and receiver readiness.
- TS60 live receive is reported by the operator as something that previously worked in the field and now does not.

## Native Code History Findings

### Relevant commits

- `639ea40` — initial single-screen collector UI
- `5098c30` — lifecycle tightening
- `d0f7452` (`v1.1.0`) — receive fix + batch import from instrument
- `d231e3e` (`v1.2.0`) — selection unlock, raw file import, copyable preview, save to local
- `641222e` — repo sanitization / rename pass on top of v1.2.0 state

### Live receive evolution

#### `v1.0.0` shape (`639ea40`)

- live receive used `drainIncomingBytes()`
- `drainIncomingBytes()` depended on `InputStream.available()`
- receive loop polled every `250ms`

#### `v1.1.0` shape (`d0f7452`)

- `BluetoothConnectionManager` gained `blockingReadBytes()`
- live receive switched from polling `available()` to blocking `read()`
- import mode used `blockingReadBytesWithTimeout()`
- this change was introduced specifically as a receive-path fix in repository history

#### `v1.2.0+` shape (`d231e3e` → current)

- batch import split away from `receiveLoop(importMode = true)` into a dedicated `rawFileReceiveLoop()`
- imported-file UI and save-to-local logic were added
- `CollectorUiState.isSelectionLocked()` changed from:
  - lock while `currentSession != null` or connected
  - to:
  - lock only while connected

### Current investigation hypothesis

At this stage, the repository history does **not** show a direct post-`v1.1.0` rewrite of the live receive read loop. The strongest current code-side suspicion is therefore not the low-level socket read itself, but state/orchestration drift around:

- current-session locking
- selection mutation while disconnected
- imported-file / save-to-local feature additions
- UI and session expectations diverging from the actual restored-session contract

## Protocol / Documentation Findings

### TS09 / FlexLine

Source used:

- `https://docs.onepointsurvey.com/pdf/Leica-FlexLine-User-Manual.pdf`

Observed facts:

- FlexLine exposes communication settings including `Port: RS232`, `Bluetooth`, and `Automatically`
- the manual includes data transfer sections for export/import and Bluetooth use
- this supports the interpretation that TS09 batch import over Bluetooth is part of the official FlexLine workflow surface

### TS60 / Captivate

Sources used:

- `https://www.manualslib.com/manual/1888980/Leica-Captivate.html?page=135`
- `https://www.manualslib.com/manual/1888980/Leica-Captivate.html?page=157`
- `https://www.manualslib.com/manual/1888980/Leica-Captivate.html?page=170`
- `https://www.manualslib.com/manual/1888980/Leica-Captivate.html?page=211`
- `https://www.manualslib.com/manual/1888980/Leica-Captivate.html?page=217`

Observed facts:

- Captivate documents `Export ASCII (Format File)` as a formal export workflow
- `Export ASCII (Format File)` requires:
  - a format file already loaded into internal memory
  - a chosen target device
  - a chosen export folder
  - the currently configured `RS232 interface` when exporting to `RS232`
- Captivate documents serial-style connection parameters for cable/Bluetooth:
  - baud rate
  - parity
  - data bits
  - stop bit
  - flow control
- Captivate documents a distinct `GSI output` connection and a distinct `GeoCOM Connection`

### Community protocol implementation evidence

Sources used:

- `https://geocompy.readthedocs.io/stable/connections/`
- `https://geocompy.readthedocs.io/v0.7.0/api/gsi/`
- `https://github.com/MrClock8163/GeoComPy`
- `https://github.com/siyka-au/pygeocom`

Observed facts:

- Leica Bluetooth communication is treated as Bluetooth Classic SPP, כלומר serial-line emulation
- GeoComPy explicitly distinguishes `GeoCOM` from `GSI Online`
- GeoCOM supports transaction IDs and checksums; GSI Online does not
- community implementations do not treat Leica total station communication as one generic text stream

## Working Conclusions Before Refactor

1. TS09 and TS60 cannot be treated as one identical batch-import path.
2. TS60 `ASCⅡ数据（格式文件）` over Bluetooth-as-RS232 must be treated as a protocol/configuration investigation problem, not merely a parser bug.
3. The live receive regression is more likely to be in state ownership / session behavior than in the current low-level blocking read implementation.

## Decision Gates

### Refactor vs rewrite gate

Proceed with in-place refactor only if:

- live receive regression can be explained by state/session/UI drift
- transport ownership can be made explicit without exploding the coordinator layer

Escalate to targeted rewrite if:

- fixes require continued shared mutable state across live receive, batch import, and UI destination state
- current coordinator boundaries cannot be made explicit without large-scale hidden coupling

### TS60 batch import gate

Continue pursuing supported TS60 batch import only if investigation produces:

- a reproducible operator workflow
- evidence that bytes begin flowing toward the receiver
- no known incompatibility that rules the path out for this release

Otherwise, this release must ship TS60 batch import as `GUIDANCE_ONLY` or `UNSUPPORTED` for the tested path and state that clearly in product/UI/docs.

## Current Implementation Outcome

### UI / Product Behavior

- The app now uses a two-page bottom-navigation layout:
  - `蓝牙`
  - `数据`
- Bluetooth-only actions remain on the Bluetooth page
- receive/import/export/file actions remain on the Data page
- the imported-file panel is now always visible, with an explicit empty state when no imported file exists

### State / Session Behavior

- brand/model/device selection is locked again when a current session exists
- switching to a different device while a current session exists now requires clearing the current session instead of silently wiping state during reconnect
- imported raw files are persisted independently from the live current session contract
- TS60 batch import no longer enters the same active import path as TS09 by default; it now surfaces guidance/limitations instead

### Verification Performed In This Environment

Confirmed green:

- targeted import-profile and imported-artifact unit tests
- targeted selection-lock / import-guidance / state-contract unit tests
- `:app:assembleDebugAndroidTest`
- `:app:lintDebug`
- `:app:assembleRelease`

Observed limitation:

- a full `:app:testDebugUnitTest` run did not reliably terminate in the current harness even after the targeted tests were green; this appears related to coroutine/test-harness interaction in the ViewModel test suite rather than a compile or lint failure
- because no connected Android device or emulator is available here, no connected instrumentation or field-level verification was completed in this environment

### Release Position

- TS09 batch import: supported baseline
- TS60 live receive: code path preserved and state/session regressions addressed, but still requires field confirmation on the real device path
- TS60 batch import: guidance-only / limitations path for this release unless future field evidence proves a reproducible supported whole-file workflow
