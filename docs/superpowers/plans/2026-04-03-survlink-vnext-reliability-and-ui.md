# SurvLink vNext Reliability And UI Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore Leica live receive reliability, introduce model-aware batch import behavior for TS09/TS60, and replace the crowded single-screen UI with a two-page Bluetooth/Data workflow.

**Architecture:** Keep the existing Kotlin/Compose/Room foundation, but split the current over-coupled collector flow into explicit Bluetooth management, live receive, and batch import lanes. Preserve one persisted current session for live records, persist imported-file metadata separately, and move UI to a bottom-navigation shell with dedicated Bluetooth and Data pages.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, AndroidX Activity/Lifecycle, Room, coroutines, StateFlow, classic Bluetooth SPP (`BluetoothAdapter`, `BluetoothDevice`, `BluetoothSocket`), FileProvider, JUnit4, Robolectric, Compose UI test

---

## File Structure

### Modify

- `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorRoute.kt`
- `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModel.kt`
- `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorUiState.kt`
- `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorScreen.kt`
- `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/BluetoothConnectionManager.kt`
- `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/BluetoothSessionPolicy.kt`
- `app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportedFileInfo.kt`
- `app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModelTest.kt`
- `app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorScreenTest.kt`
- `app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorWorkflowTest.kt`
- `README.md`

### Create

- `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorDestination.kt`
- `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/BluetoothPage.kt`
- `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/DataPage.kt`
- `app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportProfile.kt`
- `app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportProfileRegistry.kt`
- `app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportedArtifactStore.kt`
- `app/src/test/java/com/unforgettable/bluetoothcollector/data/import_/ImportProfileRegistryTest.kt`
- `app/src/test/java/com/unforgettable/bluetoothcollector/data/import_/ImportedArtifactStoreTest.kt`
- `docs/superpowers/verification/2026-04-03-ts09-ts60-field-behavior.md`

### Optional Split If `CollectorViewModel.kt` Stays Too Large

- `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/LiveReceiveCoordinator.kt`
- `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/BatchImportCoordinator.kt`

If the view model remains readable after the refactor, keep those concerns private and do not create extra files just for ceremony. If it stays bloated, split them.

## Chunk 0: Root-Cause Investigation And Release Decision Gate

### Task 0: Investigate the real-time regression and the TS60 batch-import path before refactoring

**Files:**
- Modify: `docs/superpowers/verification/2026-04-03-ts09-ts60-field-behavior.md`
- Modify later as needed: `app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModelTest.kt`

- [ ] **Step 1: Compare known-good and current native revisions**

Inspect these commits at minimum:

- `639ea40`
- `5098c30`
- `d0f7452`
- `d231e3e`
- `641222e`

Focus on:

- live receive loop changes
- import loop changes
- state-locking changes
- test drift introduction

- [ ] **Step 2: Record the real-time receive regression hypothesis matrix**

Write down, in the verification note, the candidate buckets from the spec:

- parser/buffer behavior
- idle drain interaction
- state orchestration/regression
- transport ownership or disconnect handling
- UI event wiring drift

- [ ] **Step 3: Record the TS60 batch-import candidate buckets**

Write down:

- transport mismatch
- RS232/Bluetooth configuration mismatch
- format-file dependency mismatch
- Captivate export-mode mismatch
- protocol-family mismatch

- [ ] **Step 4: Add one failing regression test that captures the identified live-receive bug shape**

If the exact cause is not fully proven yet, encode the closest reproducer as a failing test before changing production code.

- [ ] **Step 5: Make the refactor-vs-rewrite decision gate explicit**

Decision rule:

- if the regression cause can be corrected while preserving focused boundaries in the current structure, stay with in-place refactor
- if the regression cause reveals continued shared-state entanglement between live receive, import, and UI state, escalate to targeted rewrite of the coordinator layer before further feature work

Record the decision in `docs/superpowers/verification/2026-04-03-ts09-ts60-field-behavior.md`.

- [ ] **Step 6: Make the TS60 support decision gate explicit**

Continue pursuing supported TS60 batch import only if investigation yields:

- a reproducible operator workflow
- evidence that bytes begin flowing toward the receiver
- no known incompatibility that rules the path out for this release

Otherwise pivot to `GUIDANCE_ONLY` or `UNSUPPORTED` for this release and lock that outcome before UI work depends on it.

- [ ] **Step 7: Commit the investigation baseline**

```bash
git add docs/superpowers/verification/2026-04-03-ts09-ts60-field-behavior.md app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModelTest.kt
git commit -m "test: lock vnext regression investigation baseline"
```

## Chunk 1: Contracts And Regression Guards

### Task 1: Lock import-profile and imported-artifact behavior in unit tests

**Files:**
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportProfile.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportProfileRegistry.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportedArtifactStore.kt`
- Create: `app/src/test/java/com/unforgettable/bluetoothcollector/data/import_/ImportProfileRegistryTest.kt`
- Create: `app/src/test/java/com/unforgettable/bluetoothcollector/data/import_/ImportedArtifactStoreTest.kt`
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportedFileInfo.kt`

- [ ] **Step 1: Write the failing import-profile tests**

Cover at least:

```kotlin
@Test
fun ts09_profile_is_supported_for_batch_import()

@Test
fun ts60_profile_defaults_to_guidance_or_unsupported_until_supported_path_is_verified()

@Test
fun unknown_model_falls_back_to_unsupported()
```

- [ ] **Step 2: Run the new tests to confirm they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*ImportProfileRegistryTest"
```

Expected:

- test task runs
- new tests fail because the profile registry does not exist yet

- [ ] **Step 3: Write the failing imported-artifact lifecycle tests**

Cover:

```kotlin
@Test
fun imported_artifact_survives_reload()

@Test
fun failed_import_does_not_replace_last_successful_artifact()

@Test
fun clear_current_session_does_not_delete_imported_artifact()
```

- [ ] **Step 4: Run the lifecycle tests to confirm they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*ImportedArtifactStoreTest"
```

Expected:

- tests fail because there is no persistent artifact store yet

- [ ] **Step 5: Implement the minimal profile and artifact-store layer**

Implementation notes:

- use a small persisted metadata file under app files directory instead of adding a new Room table
- keep artifact persistence independent from the current session repository
- encode profile verdicts as explicit enum values: `SUPPORTED`, `GUIDANCE_ONLY`, `UNSUPPORTED`

- [ ] **Step 6: Re-run the new tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*ImportProfileRegistryTest" --tests "*ImportedArtifactStoreTest"
```

Expected:

- both test classes pass

- [ ] **Step 7: Commit the contract layer**

```bash
git add app/src/main/java/com/unforgettable/bluetoothcollector/data/import_ app/src/test/java/com/unforgettable/bluetoothcollector/data/import_
git commit -m "feat: add import profile and imported artifact contracts"
```

### Task 2: Repair drifted screen tests and encode the new UI contract

**Files:**
- Modify: `app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorScreenTest.kt`
- Modify: `app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorWorkflowTest.kt`
- Modify: `app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModelTest.kt`
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorUiState.kt`

- [ ] **Step 1: Update the existing androidTests to the real `CollectorScreen` signature**

The current `androidTest` sources are stale. First, make them compile against the real function contract before adding more behavior coverage.

- [ ] **Step 2: Add failing UI tests for the new navigation shell**

Cover:

```kotlin
@Test
fun bottom_navigation_renders_bluetooth_and_data_destinations()

@Test
fun imported_file_panel_is_visible_even_when_empty()
```

- [ ] **Step 3: Add failing ViewModel tests for the new lane contracts**

Cover:

```kotlin
@Test
fun live_receive_is_refused_while_import_is_active()

@Test
fun import_is_refused_while_live_receive_is_active()

@Test
fun selection_is_locked_when_current_session_exists()

@Test
fun reconnect_to_different_device_is_refused_while_current_session_exists()

@Test
fun import_interrupted_by_disconnect_preserves_last_successful_artifact()
```

- [ ] **Step 4: Run the tests and confirm failure**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*CollectorViewModelTest" :app:assembleDebugAndroidTest
```

Expected:

- unit tests fail on missing new behavior
- androidTest sources compile-fail or test-fail until the navigation shell and empty import panel exist

- [ ] **Step 5: Implement the minimum state changes needed to make the tests compile**

Add only the smallest `CollectorUiState` surface needed for:

- selected destination
- imported-file empty state
- guidance/unsupported import CTA state

- [ ] **Step 6: Re-run the same test commands**

Expected:

- unit tests pass for the newly introduced state contract
- androidTest sources compile successfully, though UI behavior will still evolve in later tasks

- [ ] **Step 7: Commit the repaired regression guard**

```bash
git add app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorUiState.kt
git commit -m "test: repair collector ui contract coverage"
```

## Chunk 2: Transport And Runtime Refactor

### Task 3: Make transport ownership explicit and restore live receive reliability

**Files:**
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/BluetoothConnectionManager.kt`
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorRoute.kt`
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModel.kt`
- Modify: `app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModelTest.kt`

- [ ] **Step 1: Add failing tests for transport arbitration**

Cover:

```kotlin
@Test
fun live_receive_acquires_transport_read_access_exclusively()

@Test
fun disconnect_revokes_active_read_access()

@Test
fun reconnect_resets_transport_session_before_new_read_owner_is_assigned()
```

- [ ] **Step 2: Run the relevant unit tests to confirm failure**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*CollectorViewModelTest"
```

Expected:

- failures show that read ownership is currently implicit and not modeled

- [ ] **Step 3: Introduce explicit transport ownership in `BluetoothConnectionManager`**

Minimal shape:

```kotlin
enum class TransportReadOwner { NONE, LIVE_RECEIVE, BATCH_IMPORT }

fun acquireReadAccess(owner: TransportReadOwner): Boolean
fun releaseReadAccess(owner: TransportReadOwner)
```

Use this to guard:

- blocking live reads
- blocking import reads
- disconnect revocation
- reconnect reset

- [ ] **Step 4: Refactor the view model to use the explicit ownership API**

Important:

- acquire before entering `liveReceiving`
- release on stop/disconnect/link loss
- never let both lanes read concurrently

- [ ] **Step 5: Run the receive regression tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*CollectorViewModelTest"
```

Expected:

- transport ownership tests pass
- existing receive regression tests still pass

- [ ] **Step 6: Commit the transport refactor**

```bash
git add app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/BluetoothConnectionManager.kt app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorRoute.kt app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModel.kt app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModelTest.kt
git commit -m "fix: make collector transport ownership explicit"
```

### Task 4: Separate live receive from batch import and preserve TS09 behavior

**Files:**
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModel.kt`
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportedFileInfo.kt`
- Modify: `app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModelTest.kt`

- [ ] **Step 1: Add failing tests for the two lane states**

Cover:

```kotlin
@Test
fun start_import_does_not_enter_live_receive_state()

@Test
fun cancel_import_preserves_last_successful_imported_file()

@Test
fun ts09_import_completes_after_post_first_byte_silence()

@Test
fun link_loss_during_awaiting_import_data_returns_to_non_import_state_without_false_success()

@Test
fun link_loss_during_import_receiving_preserves_last_successful_artifact_and_reports_interruption()
```

- [ ] **Step 2: Run those tests to confirm failure**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*CollectorViewModelTest"
```

- [ ] **Step 3: Refactor import flow into its own explicit state machine**

Required states:

- `idle`
- `awaitingImportData`
- `importReceiving`
- `importCompleted`
- `importFailed`

Required rules:

- no auto-timeout before first byte
- `3_000L` silence threshold after first byte for TS09 baseline path
- cancel/disconnect/link-loss never produce false success artifacts

- [ ] **Step 4: Re-run the same tests**

Expected:

- TS09 baseline import tests pass
- mutual exclusion and preservation behavior pass

- [ ] **Step 5: Commit the lane separation**

```bash
git add app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModel.kt app/src/main/java/com/unforgettable/bluetoothcollector/data/import_/ImportedFileInfo.kt app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModelTest.kt
git commit -m "fix: separate live receive from batch import flow"
```

## Chunk 3: Two-Page UI Refactor

### Task 5: Replace the single crowded screen with bottom navigation and split pages

**Files:**
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorDestination.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/BluetoothPage.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/DataPage.kt`
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorScreen.kt`
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorRoute.kt`
- Modify: `app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorScreenTest.kt`
- Modify: `app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorWorkflowTest.kt`

- [ ] **Step 1: Write or extend failing Compose tests for page ownership**

Cover:

```kotlin
@Test
fun bluetooth_page_shows_brand_model_pair_connect_disconnect_controls()

@Test
fun data_page_shows_receive_import_preview_export_and_imported_file_panel()

@Test
fun preview_rows_remain_selection_copy_capable()

@Test
fun imported_file_actions_are_visually_separate_from_session_export_actions()
```

- [ ] **Step 2: Run the androidTest compile step**

Run:

```bash
./gradlew :app:assembleDebugAndroidTest
```

Expected:

- compile/test failures until the new page files exist

- [ ] **Step 3: Create the bottom navigation shell**

Keep the root shell responsible for:

- destination switching
- passing correct callbacks
- retaining the export dialog

Move page content into:

- `BluetoothPage.kt`
- `DataPage.kt`

- [ ] **Step 4: Move Bluetooth-only controls into `BluetoothPage.kt`**

This page owns:

- brand/model selectors
- nearby devices
- paired devices
- search start/stop
- pair
- connect
- disconnect
- connection/permission summaries

- [ ] **Step 5: Move data-only controls into `DataPage.kt`**

This page owns:

- current session summary
- start/stop receive
- import CTA/guidance CTA
- imported-file panel
- preview list
- export/share
- save-to-local
- clear current session

- [ ] **Step 6: Re-run Compose and androidTest compile checks**

Run:

```bash
./gradlew :app:assembleDebugAndroidTest
```

Expected:

- androidTest sources compile

- [ ] **Step 7: Commit the UI split**

```bash
git add app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector
git commit -m "feat: split collector flow into bluetooth and data pages"
```

### Task 6: Make statuses, recovery paths, and import CTA states explicit in the UI

**Files:**
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/BluetoothPage.kt`
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/DataPage.kt`
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModel.kt`
- Modify: `app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorScreenTest.kt`

- [ ] **Step 1: Add failing tests for Bluetooth error recovery UX**

Cover:

```kotlin
@Test
fun bluetooth_page_surfaces_permission_denied_adapter_off_and_connect_failure()

@Test
fun unsupported_or_guidance_only_import_profile_never_starts_import()
```

- [ ] **Step 2: Run the tests and confirm failure**

Run:

```bash
./gradlew :app:assembleDebugAndroidTest :app:testDebugUnitTest --tests "*CollectorViewModelTest"
```

- [ ] **Step 3: Implement the visible status states**

Required visible statuses:

- connected idle
- live receiving
- receive blocked by missing metadata
- import waiting
- import receiving
- import cancelled
- import failed
- import interrupted
- live/import mutual-exclusion refusal
- unsupported TS60 or guidance-only path

- [ ] **Step 4: Implement the import CTA state table**

Hard-code the UI contract from the spec:

- `SUPPORTED` -> `导入存储数据`
- `GUIDANCE_ONLY` -> `查看导入说明`
- `UNSUPPORTED` -> `查看限制说明`

- [ ] **Step 5: Implement Bluetooth recovery affordances**

Explicitly cover:

- permission denied
- adapter off
- connect failure

The user must be able to see the failure reason and the next recovery action from the Bluetooth page itself.

- [ ] **Step 6: Re-run the same tests**

Expected:

- ViewModel tests pass
- androidTest compile still passes

- [ ] **Step 7: Commit the status/recovery UX**

```bash
git add app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector
git commit -m "feat: add collector status and recovery states"
```

## Chunk 4: Verification, TS60 Decision Gate, And Docs

### Task 7: Record TS09/TS60 support findings and freeze the TS60 decision gate

**Files:**
- Create: `docs/superpowers/verification/2026-04-03-ts09-ts60-field-behavior.md`
- Modify: `README.md`

- [ ] **Step 1: Update README support wording**

README must reflect:

- bottom-nav two-page UI
- TS09 batch import baseline
- TS60 batch-import support status for this release
- TS60 live receive restored status

- [ ] **Step 2: Do not finalize the verification note yet**

Only keep placeholders for headings until Task 8 evidence is gathered. The final note must be written after the verification commands and any available field/device checks complete.

- [ ] **Step 3: Commit README wording separately**

```bash
git add README.md
git commit -m "docs: update vnext support wording"
```

### Task 8: Run verification, capture evidence, and finalize the support matrix

**Files:**
- Modify: `docs/superpowers/verification/2026-04-03-ts09-ts60-field-behavior.md`

- [ ] **Step 1: Run focused unit tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*ImportProfileRegistryTest" --tests "*ImportedArtifactStoreTest" --tests "*CollectorViewModelTest"
```

Expected:

- all targeted unit tests pass

- [ ] **Step 2: Run the broader unit test suite**

Run:

```bash
./gradlew :app:testDebugUnitTest
```

Expected:

- full unit suite passes

- [ ] **Step 3: Run build + lint + androidTest compile**

Run:

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest
```

Expected:

- debug build succeeds
- lint succeeds or only reports explicitly accepted warnings
- androidTest APK assembles

- [ ] **Step 4: Verify the width matrix**

Verify the UI at:

- `360dp`
- `393dp`
- `412dp`

Capture whether any primary controls clip or wrap incorrectly.

- [ ] **Step 5: Attempt connected tests if device/emulator exists**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected:

- if no device/emulator is present, record that as an environment limitation rather than pretending the suite passed

- [ ] **Step 6: Finalize the TS09/TS60 verification note**

Record:

- TS09 working batch-import path
- TS60 live-receive regression fix evidence
- TS60 batch-import investigated path(s)
- whether this release ships TS60 batch import as `SUPPORTED`, `GUIDANCE_ONLY`, or `UNSUPPORTED`
- any environment limitations that blocked connected-device evidence

- [ ] **Step 7: Final commit for verification adjustments**

```bash
git add docs/superpowers/verification/2026-04-03-ts09-ts60-field-behavior.md README.md app
git commit -m "chore: finalize survlink vnext verification"
```

## Execution Notes

- Start with Chunk 1 and do not skip the failing-test step.
- The first hard decision point is in Task 7: TS60 batch import ships either as supported path or as explicit guidance-only/unsupported path. Do not leave it vague.
- If `CollectorViewModel.kt` remains too large after Tasks 3-6, split it before final verification rather than accepting a fragile god-object.
- Keep `.superpowers/` and `.firecrawl/` out of commits.
