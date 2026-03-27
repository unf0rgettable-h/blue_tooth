# Android Native Total Station Collector Design

## Who This Is For

This document is for the engineer who will replace the current MIT App Inventor project with a native Android Studio application while preserving only the field data collection workflow that still matters:

- choose instrument brand and model
- choose a paired Bluetooth device
- connect and receive measurement data
- save data on the phone
- preview data on the phone
- export and share saved data as `CSV` or `TXT`

It supersedes the earlier App Inventor in-place refactor plan. The new direction is a native Android rewrite, not continued maintenance of `.scm/.bky`.

## Goal

Build a stable V1 native Android application focused on Bluetooth-based total-station data intake. The product must favor maintainability, minimum verifiable behavior, and future protocol adaptation over preserving old App Inventor structures or legacy verification workflows.

## Non-Goals

The native V1 must not implement or carry forward:

- add-constant calculation
- repeatability calculation
- true-value input
- manual measurement entry
- file import into the app
- direct Bluetooth file transfer into the app as the primary transport contract
- traditional verification report generation
- USB disk / dedicated cable transfer flows
- background long-running collection service
- cloud sync
- brand-specific deep protocol decoding beyond a generic text parser

These can be considered future V2/V3 directions if still justified later.

## Product Scope

### Required V1 Capabilities

1. Instrument selection
   - brand selector
   - model selector
   - options can be seeded from the existing MIT App Inventor project
2. Nearby Bluetooth discovery
   - search nearby classic Bluetooth devices on site
   - stop discovery
   - show discovered devices with basic identity
3. Pairing flow
   - when a discovered device is not bonded, trigger the Android system pairing flow
   - refresh bonded state after pairing succeeds
4. Bluetooth device selection
   - list paired classic Bluetooth devices
   - allow reloading the paired-device list and selection
5. Connection control
   - connect
   - disconnect
   - clear state transitions and error feedback
6. Reception control
   - start receiving
   - stop receiving without forcing disconnect
7. Real-time preview
   - ordered list of received records
   - visible status and total count
8. Phone-side persistence
   - receive-and-save by default
   - current session survives app restart
9. Export and share
   - user chooses `CSV` or `TXT` at export time
   - file is generated on phone
   - share via Android system sheet

### UX Direction

Use a single main screen.

Top area is split into two logical columns on the same screen:

- left: instrument selection
  - brand
  - model
- right: Bluetooth device selection
  - nearby device list
  - start discovery / stop discovery
  - system pairing entry point for unpaired devices
  - paired device list
  - reload paired list
  - connect / disconnect

Below that:

- connection and reception status
- start receive / stop receive / clear current session / export
- receive count
- real-time preview list

No secondary workflow screens are required in V1.

### State Constraints

To keep metadata stable and keep the first version simple:

- if a current session exists, brand, model, and Bluetooth device selection remain locked
- selectors unlock only after `Clear Current Session`
- once connected, those selectors are also read-only
- to change instrument metadata or target device, the user must disconnect first and clear the current session
- nearby discovery is active scanning and is distinct from reloading the paired-device list
- reloading the paired-device list is only a fresh query of already bonded devices
- discovery is allowed only while disconnected
- if discovery is in progress and the user initiates connect, discovery must be cancelled first
- V1 connection attempts are made only against bonded devices; unbonded devices must first go through the Android system pairing flow
- if the session's selected target device loses its bond, the app may allow re-pairing that same saved device identity without clearing the current session

## Architecture

## Module Strategy

Create a standard Android Studio project with a single app module. Keep internal boundaries clear, but do not over-engineer.

Recommended package structure:

- `ui.collector`
  - main screen
  - dialogs / sheets
  - view state rendering
- `domain.model`
  - `InstrumentBrand`
  - `InstrumentModel`
  - `DiscoveredBluetoothDeviceItem`
  - `BluetoothDeviceItem`
  - `MeasurementRecord`
  - `ExportFormat`
- `data.instrument`
  - instrument catalog source
- `data.bluetooth`
  - Bluetooth discovery
  - pairing handoff handling
  - Bluetooth connection and receive pipeline
- `data.storage`
  - session persistence
  - export file generation
- `data.share`
  - Android share intents and `FileProvider`

## Platform Choice

Use native Android with Kotlin. Favor modern Android patterns and lifecycle-safe state handling.

Jetpack Compose is preferred for UI because:

- the app is effectively a fresh rewrite
- state-driven UI is a better match for connection and receive status changes
- later UI iteration will be cheaper

This is a preference, not a hard business requirement. If local repo constraints require XML views, keep the same boundaries.

## Data Model

The core record should carry the original measurement payload plus enough metadata for traceability and export.

`MeasurementRecord` should contain at least:

- `id`
- `sequence`
- `receivedAt`
- `instrumentBrand`
- `instrumentModel`
- `bluetoothDeviceName`
- `bluetoothDeviceAddress`
- `rawPayload`
- `parsedCode`
- `parsedValue`

V1 also requires a minimal persisted session model.

`Session` should contain at least:

- `sessionId`
- `startedAt`
- `updatedAt`
- `instrumentBrand`
- `instrumentModel`
- `bluetoothDeviceName`
- `bluetoothDeviceAddress`
- `delimiterStrategy`
- `isCurrent`

Parsing must stay intentionally light in V1.

- if a cleaned text record can be interpreted as `code + value`, store both
- if it cannot be interpreted but is still valid text, preserve the raw payload and leave parsed fields blank
- only empty fragments, control-character garbage, and overflow fragments that cannot be recovered are dropped

## Session Semantics

V1 has exactly one persisted current session.

- the current session is created when the user presses `Start Receive` and no current session exists
- session metadata is immutable once the session is created
- if the app restarts, the same current session data is restored, but Bluetooth connection state is not restored
- after app restart, the app returns in a disconnected, not-receiving state and the user must reconnect and press `Start Receive` again
- if the user disconnects and reconnects to the same instrument and same Bluetooth device, new records continue to append to the same current session
- if the same saved target device loses bond state, the app may re-enter the Android system pairing flow for that same device and then continue the current session after bonding is restored
- if the user wants to change brand, model, or Bluetooth target device, the current session must be cleared first
- `Clear Current Session` deletes only the current session and its persisted records
- V1 export scope is always the current session
- long-term retention for V1 is provided by exported files, not by a separate in-app history browser

This keeps V1 minimal while still leaving room for a V2 session-history feature later.

## Receive Pipeline

The device side is assumed to provide classic Bluetooth text output, not a complete file-transfer protocol. Therefore the stable approach is:

1. connect to the Bluetooth device
2. start reading the text stream
3. accumulate data into a buffer
4. split only complete records
5. convert complete records into `MeasurementRecord`
6. save each accepted record immediately
7. refresh preview from saved session state

This is intentionally not “stream only in memory”. It is “stream receive plus immediate phone-side persistence”.

### Bluetooth Connection Assumptions

V1 makes the following explicit transport assumptions:

- target transport is classic Bluetooth RFCOMM / SPP-style communication
- V1 uses the standard Serial Port Profile UUID: `00001101-0000-1000-8000-00805F9B34FB`
- V1 supports on-site discovery of nearby devices, but only bonded devices enter the connection path
- V1 uses Android system pairing rather than custom in-app pairing UX
- connection attempts use a bounded timeout and can be cancelled by the user
- if a device does not speak the expected SPP-style transport, V1 may fail to connect and that is treated as unsupported for this release

Recommended defaults for implementation:

- discovery is bounded and explicit; the app exposes start / stop discovery controls
- connect timeout: `10 seconds`
- read loop cancellation on explicit disconnect or app shutdown
- a failed connection attempt must return the app to a clean disconnected state
- discovery and RFCOMM connection are mutually exclusive; the app must cancel discovery before connecting
- connected but not yet started receiving uses the same drain-and-discard behavior as paused state, so the device is not blocked by unread data

### Buffering Rules

- receiving must happen off the main thread
- V1 does not auto-detect record boundaries at runtime
- each instrument model declares one explicit delimiter strategy in configuration
- supported V1 delimiter strategies are:
  - `LINE_DELIMITED`: record boundary is `\r`, `\n`, or `\r\n`
  - `WHITESPACE_TOKEN`: each non-empty token separated by whitespace is one record
- the active parser uses only the selected model's configured delimiter strategy
- partial fragments must remain in the buffer until completed
- `LINE_DELIMITED` splits only on line endings and may trim surrounding whitespace after a line is isolated
- `WHITESPACE_TOKEN` treats whitespace as token delimiters, including spaces, tabs, and repeated whitespace
- the pending text buffer must have a hard cap to avoid unbounded growth
- when the buffer exceeds the cap without yielding a complete record, the implementation must discard the oldest undecodable pending bytes, keep the newest tail, and surface a non-fatal overflow warning

Recommended defaults for implementation:

- pending buffer cap: `32 KB`
- retained tail after overflow: `8 KB`

### Stability Rules

- one bad token must not kill the session
- stop receive must pause record persistence and preview updates, not auto-disconnect
- while paused, the app should keep the socket alive and continue draining incoming bytes in the background so the device is not blocked by unread data
- bytes drained while paused are intentionally discarded in V1
- when `Stop Receive` is pressed, any incomplete buffered fragment is discarded and a non-fatal warning may be shown
- `Clear Current Session` is allowed only while disconnected
- if the user requests clear while connected or receiving, the app must refuse the action and instruct the user to stop receiving if needed, then disconnect, and then clear
- disconnect must preserve already saved session data
- unexpected disconnect must surface a user-visible state and allow reconnection

## Persistence Strategy

V1 must default to receive-and-save.

V1 uses a single persisted current session. There is no separate in-app historical log model in this release.

Implementation choice should prioritize simplicity and reliability. A local database such as Room is preferred because it gives:

- structured query access
- process-death resilience
- easier testability
- easier future expansion

If needed, a plain local file log may also be generated in addition to structured storage, but structured local persistence is the better primary source of truth for the current session.

## Export And Sharing

The user should not choose the format upfront in settings. Export time is the right place to choose.

When the user taps export:

1. show format chooser
   - `CSV`
   - `TXT 原始日志`
2. generate file from the persisted current session
3. store it in app-managed phone storage
4. trigger Android system sharing

### CSV Export

CSV must include at least:

- sequence
- received time
- instrument brand
- instrument model
- Bluetooth device name
- Bluetooth device address
- raw payload
- parsed code
- parsed value

CSV export rules:

- encoding: `UTF-8`
- delimiter: comma
- quoting: standard CSV quoting for fields that contain commas, quotes, or newlines
- line ending: `CRLF`
- timestamp format: ISO 8601 with timezone offset, for example `2026-03-25T14:22:31.123+08:00`
- file naming: timestamp-based unique file names, never overwrite an existing export

### TXT Export

TXT export is the raw-oriented log format. It should remain easy to forward to technicians or preserve as evidence. Include readable headers plus the raw payload records in receive order.

TXT export rules:

- encoding: `UTF-8`
- line ending: `CRLF`
- include a small header block with session start time, instrument brand/model, Bluetooth device name/address, and export time
- file naming: timestamp-based unique file names, never overwrite an existing export

### Sharing

Use standard Android file sharing:

- app-owned exported file
- `FileProvider`
- `ACTION_SEND`

The export step and the share step should be separate in code, even if they occur back-to-back in the user flow.

## Permissions

Choose the Android version baseline for stability first, not for chasing the newest API level.

Recommended release targets:

- `minSdk = 26`
- `compileSdk = current stable SDK available at implementation time`
- `targetSdk = current stable target used by the project at implementation time`

Version strategy:

- do not set the product contract to “Android 15+ only” for V1 unless deployment constraints later prove that is truly acceptable
- prioritize correctness on Android 12+ because the Bluetooth permission model changes materially there
- if field phones are mostly newer devices, focus manual verification on Android 13, 14, and 15, but keep runtime compatibility broader where practical

Bluetooth permission handling must explicitly cover:

- Android 12+ runtime permissions for nearby-device discovery and connection
- API 26-30 compatibility requirements for classic Bluetooth discovery and connection
- the case where system Bluetooth is turned off before connect or during a session
- the case where discovery permission is granted but connection permission is denied, and vice versa

Concrete pre-Android-12 rule:

- on Android 11 and lower, nearby classic Bluetooth discovery is blocked unless the app has the required location permission for discovery
- V1 must explicitly request `ACCESS_FINE_LOCATION` on API 26-30 when discovery is used
- if that permission is denied, the app must keep connect-to-bonded-device behavior available where possible, but show discovery as unavailable with a clear reason

Guidelines:

- request only what is actually needed
- handle denial with clear UI state
- do not let missing permission crash the app or leave it in an ambiguous half-connected state

## Maintainability Priorities

The implementation must optimize for:

1. clear separation between UI, Bluetooth IO, storage, and export
2. minimal but explicit state machine for connection and receiving
3. testable parsing and export logic
4. easy extension of instrument catalog and parser strategies

Avoid:

- UI code owning Bluetooth details
- business logic embedded inside export flows
- hard-coded assumptions about a single brand protocol
- deep architecture layers that bring ceremony without payoff

## Instrument Catalog Requirements

The instrument catalog is not just display data. It is configuration that drives parsing.

Each model definition should carry at least:

- `brandId`
- `modelId`
- `displayName`
- `delimiterStrategy`

Optional but recommended model metadata:

- `expectedTransport = CLASSIC_BLUETOOTH_SPP`
- `notes` for device-specific field guidance

V1 delimiter strategy is selected from configuration, not inferred from incoming data. This makes the receive path more stable and keeps later model-specific adaptation straightforward.

## Minimum Verifiable Behavior

The native V1 is acceptable only if the following path can be verified end to end:

1. select brand and model
2. search nearby devices
3. pair one unbonded device through the Android system pairing flow
4. confirm that device appears in the paired-device list
5. connect successfully
6. start receiving
7. see records appear in preview
8. stop receiving
9. restart app and confirm current session is still present
10. export as `CSV`
11. export as `TXT`
12. invoke Android share sheet for either file

## Testing Strategy

Prioritize small, high-signal tests over broad ceremony.

### Unit Tests

Add unit coverage for:

- buffer splitting and partial-fragment handling
- record cleaning and light parsing
- CSV generation
- TXT generation

### UI / Integration Checks

Perform targeted manual validation for:

- permission grant / denial
- discovery start / stop
- pairing success / cancellation / failure
- connect / disconnect state changes
- start / stop receive
- abnormal input handling
- export and share invocation

## Future V2 / V3 Directions

These are intentionally deferred, but the design should not block them:

- brand-specific parser strategies
- richer session/history filtering
- background collection
- automatic reconnection policies
- cloud backup or sync
- additional export formats
- direct Bluetooth file-transfer adapters for specific instrument families if a documented, stable device path later justifies them
- non-Bluetooth input sources such as USB or file-based imports

## Migration Note

The old MIT App Inventor repository remains the behavior reference for:

- original brand and model options
- the fact that the current device communication is text-stream based
- existing user mental model around instrument and Bluetooth selection

It is not the source of truth for architecture, persistence, or export behavior in the native rewrite.
