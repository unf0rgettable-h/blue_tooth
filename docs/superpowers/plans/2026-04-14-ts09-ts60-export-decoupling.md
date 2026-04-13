# TS09 / TS60 Export Decoupling Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Decouple TS09 client-import and TS60 receiver-import paths so they no longer share import orchestration, fix the TS09 delayed file receipt bug, and make the TS60 export-to-phone path the selected model’s real primary action.

**Architecture:** Keep live measurement on the existing protocol lane, but split batch file import into two explicit lanes. TS09 gets a dedicated client-import coordinator with deterministic silence detection based on draining available bytes after the first blocking read. TS60 gets a receiver/import lane selected from the profile and routed through discoverable + RFCOMM receiver flow instead of guidance-only copy.

**Tech Stack:** Kotlin, Android Bluetooth Classic, Jetpack Compose, JUnit, existing CollectorViewModel/CollectorRoute architecture.

---

## Chunk 1: Decouple profile routing and UI action selection

### Task 1: Make TS60 a real receiver-import profile

**Files:**
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportProfile.kt`
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportProfileRegistry.kt`
- Test: `app/src/test/java/com/unforgettable/bluetoothcollector/data/import_/ImportProfileRegistryTest.kt`

- [ ] Add an explicit experimental verdict for TS60 receiver mode.
- [ ] Switch TS60 profile to `RECEIVER_STREAM` with TS60-specific export-to-phone wording.
- [ ] Update profile tests first, run them red, then green.

### Task 2: Route the Data-page primary action from profile execution mode

**Files:**
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorScreen.kt`
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorUiState.kt`
- Test: `app/src/test/java/com/unforgettable/bluetoothcollector/data/import_/ImportProfileRegistryTest.kt`

- [ ] Make the primary import/export-to-phone button call client import for `CLIENT_STREAM` and receiver preparation for `RECEIVER_STREAM`.
- [ ] Show the receiver diagnostics panel only for receiver-mode profiles (not all Captivate models by firmware family).
- [ ] Keep TS09 and TS60 buttons independent at the UI routing level.

## Chunk 2: Extract and fix the TS09 client-import lane

### Task 3: Introduce a dedicated TS09 client-import coordinator

**Files:**
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/BluetoothClientImportManager.kt`
- Test: `app/src/test/java/com/unforgettable/bluetoothcollector/data/import_/BluetoothClientImportManagerTest.kt`

- [ ] Write failing tests for silence-based completion after first export bytes arrive.
- [ ] Implement polling-based silence detection using first blocking read + repeated `drainIncomingBytes()`.
- [ ] Ensure the imported file is persisted without needing a second export to unblock the read loop.

### Task 4: Replace ViewModel-owned TS09 raw import loop with the coordinator

**Files:**
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModel.kt`
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorRoute.kt`
- Modify: `app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModelTest.kt`

- [ ] Inject the new client-import controller.
- [ ] Use it only for `CLIENT_STREAM` profiles.
- [ ] Keep stop/disconnect behavior safe for TS09 imports.
- [ ] Add/adjust focused ViewModel tests around TS09 import and TS60 non-client routing.

## Chunk 3: Harden the TS60 receiver lane

### Task 5: Make receiver mode a TS60-owned primary import/export lane

**Files:**
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModel.kt`
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorScreen.kt`
- Modify: `app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModelTest.kt`

- [ ] Guard receiver start so TS09 cannot enter it accidentally.
- [ ] Keep discoverable/connect/listen logging and diagnostics on the TS60 lane.
- [ ] Preserve the experimental label while making the path actionable.

## Chunk 4: Verification

### Task 6: Run focused then broader verification

**Files:**
- Test: `app/src/test/java/com/unforgettable/bluetoothcollector/data/import_/BluetoothClientImportManagerTest.kt`
- Test: `app/src/test/java/com/unforgettable/bluetoothcollector/data/import_/ImportProfileRegistryTest.kt`
- Test: `app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModelTest.kt`

- [ ] Run focused profile/import tests.
- [ ] Run focused ViewModel tests.
- [ ] Run the full `:app:testDebugUnitTest` suite.
- [ ] Summarize changed files, simplifications made, and remaining risks.
