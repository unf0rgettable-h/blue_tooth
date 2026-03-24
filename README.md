# blue_tooth

Source-only repository for an MIT App Inventor Android project that automates total station distance-data intake, add-constant calculation, repeatability calculation, and report export.

This repository intentionally keeps only the application source and minimal project assets. Academic writeups, internal analysis notes, and tracked APK binaries are excluded from the git tree.

## Repository Scope

- MIT App Inventor source files for a 4-screen Android app
- Project metadata and packaged assets required by the source tree
- No thesis document in git
- No working analysis document in git
- No APK stored in the repository history after cleanup

## Source Layout

```text
assets/
  6a63a07d00c191655327438745601567.jpg   App icon

src/appinventor/ai_xiakele341/blue_tooth/
  Screen1.scm / Screen1.bky              Main control screen
  Screen2.scm / Screen2.bky              Data view and single-point correction
  Screen3.scm / Screen3.bky              Reference-value input
  Screen4.scm / Screen4.bky              Manual/file-based distance entry

youngandroidproject/
  project.properties                     App metadata and build settings
```

## What The App Does

- Connects to paired total station devices over classic Bluetooth
- Polls incoming text data with an App Inventor `Clock` component
- Parses measurement tokens into point IDs and distance values
- Expands six stored reference values into twenty-one baseline combinations
- Computes add constant `K` and scale term `R`
- Computes distance repeatability from a measurement set
- Stores shared state with `TinyDB`
- Exports text reports from the mobile app

## Screen Overview

### Screen1

Primary runtime screen for:

- Bluetooth connection management
- Incoming data parsing
- Add-constant calculation
- Repeatability calculation
- Report export

### Screen2

Review screen for received data. Supports list-based inspection and single-item correction, then writes corrected values back to `TinyDB`.

### Screen3

Input screen for six reference values used to generate the baseline set for add-constant calculation. Values can be typed manually or loaded from a text file.

### Screen4

Fallback data-entry screen for manual input or file import. It can be used when Bluetooth transfer is unavailable or when a full measurement set needs to be re-entered.

## Technical Notes

- Platform: MIT App Inventor
- Main entry screen: `Screen1`
- Storage: `TinyDB`
- Bluetooth mode: classic Bluetooth via `BluetoothClient`
- Receive loop: 500 ms polling timer
- Current report format: plain text files
- App version in source metadata: `1.0`

## Working With The Source

This repository contains the extracted App Inventor project structure rather than a bundled `.aia` archive. If you need to inspect or migrate the project, start from:

- `youngandroidproject/project.properties`
- `src/appinventor/ai_xiakele341/blue_tooth/`
- `assets/`

## Releases

Prebuilt APK files, when published, are attached to GitHub Releases instead of being committed into the repository.
