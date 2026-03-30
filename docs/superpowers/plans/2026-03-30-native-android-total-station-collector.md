# Native Android Total Station Collector Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a native Android Studio app that supports nearby Bluetooth discovery, system pairing, bonded-device connection, measurement text-stream intake, phone-side persistence, on-phone preview, and CSV/TXT export/share for total-station field work.

**Architecture:** Create a new Kotlin Android Studio project in this repo while keeping the existing MIT App Inventor sources as legacy reference material. The app should use a single-screen Compose UI, a small Room-backed persistence layer, a classic Bluetooth discovery/connection stack, and an export/share pipeline that generates files on the phone from persisted session data.

**Tech Stack:** Kotlin, Android Studio, Jetpack Compose, AndroidX, Room, coroutines, StateFlow, classic Bluetooth (`BluetoothAdapter`, `BluetoothDevice`, `BluetoothSocket`), FileProvider, JUnit, Compose UI test

---

## File Structure

### New Android Project Files

- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradle/wrapper/gradle-wrapper.jar`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/xml/file_paths.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml` or point to existing asset strategy
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/MainActivity.kt`

### Domain / Config

- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/InstrumentBrand.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/InstrumentModel.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/DelimiterStrategy.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/Session.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/MeasurementRecord.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/DiscoveredBluetoothDeviceItem.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/BondedBluetoothDeviceItem.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/ExportFormat.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/instrument/InstrumentCatalog.kt`

### Bluetooth Layer

- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/BluetoothSessionPolicy.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/BluetoothDiscoveryManager.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/BondedDeviceManager.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/BluetoothConnectionManager.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/BluetoothPermissionChecker.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/PairingRequestCoordinator.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/TextStreamRecordParser.kt`

### Persistence / Export / Sharing

- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/storage/AppDatabase.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/storage/SessionEntity.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/storage/MeasurementRecordEntity.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/storage/SessionDao.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/storage/MeasurementRecordDao.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/storage/CollectorRepository.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/export/CsvExportWriter.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/export/TxtExportWriter.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/export/ExportFileNamer.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/share/ShareLauncher.kt`

### UI / State

- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModel.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorUiState.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorRoute.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorScreen.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/ExportFormatDialog.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/theme/Theme.kt`

### Tests

- Create: `app/src/test/java/com/unforgettable/bluetoothcollector/data/instrument/InstrumentCatalogTest.kt`
- Create: `app/src/test/java/com/unforgettable/bluetoothcollector/data/bluetooth/BluetoothSessionPolicyTest.kt`
- Create: `app/src/test/java/com/unforgettable/bluetoothcollector/data/bluetooth/TextStreamRecordParserTest.kt`
- Create: `app/src/test/java/com/unforgettable/bluetoothcollector/data/storage/CollectorRepositoryTest.kt`
- Create: `app/src/test/java/com/unforgettable/bluetoothcollector/data/export/CsvExportWriterTest.kt`
- Create: `app/src/test/java/com/unforgettable/bluetoothcollector/data/export/TxtExportWriterTest.kt`
- Create: `app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModelTest.kt`
- Create: `app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorScreenTest.kt`
- Create: `app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorWorkflowTest.kt`

### Docs

- Modify: `README.md`
- Preserve as legacy reference: `src/appinventor/ai_xiakele341/blue_tooth/*`
- Preserve as legacy reference: `youngandroidproject/project.properties`

## Chunk 1: Project Bootstrap And Bluetooth Foundation

### Task 1: Create the Android Studio project skeleton

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradle/wrapper/gradle-wrapper.jar`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/xml/file_paths.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/MainActivity.kt`

- [ ] **Step 1: Create the root Gradle settings**

Use a standard single-app Kotlin Android project.

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "bluetoothcollector"
include(":app")
```

- [ ] **Step 2: Create the version catalog**

Include only dependencies needed for V1:

- Android Gradle Plugin
- Kotlin
- Compose BOM
- Activity Compose
- Lifecycle ViewModel Compose
- Room
- Coroutines
- JUnit
- AndroidX test libraries

- [ ] **Step 3: Create the app module build file**

Set the initial SDK targets to the stable-first policy from the spec:

```kotlin
android {
    namespace = "com.unforgettable.bluetoothcollector"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.unforgettable.bluetoothcollector"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }
}
```

Keep `compileSdk` / `targetSdk` easy to bump later without forcing a runtime support policy change.

- [ ] **Step 4: Create the root build file and Gradle wrapper**

The bootstrap is not reproducible without wrapper files. Include:

- root `build.gradle.kts` with plugin aliases declared `apply false`
- generated `gradlew` / `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper/gradle-wrapper.jar`

If the wrapper is generated rather than copied from a template, make that explicit:

```bash
gradle wrapper --gradle-version <stable-version>
chmod +x gradlew
```

- [ ] **Step 5: Add the manifest shell**

Declare:

- Bluetooth permissions for modern Android
- pre-Android-12 compatibility permissions
- location permission needed for API 26-30 discovery
- `FileProvider`
- launcher activity

- [ ] **Step 6: Add the Compose activity shell**

Create a minimal `MainActivity` that hosts `CollectorRoute()`.

- [ ] **Step 7: Run a bootstrap build**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected:

- build succeeds
- APK is produced under `app/build/outputs/apk/debug/`

- [ ] **Step 8: Commit the bootstrap**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradlew gradlew.bat gradle/wrapper gradle/libs.versions.toml app
git commit -m "feat: bootstrap native android collector project"
```

### Task 2: Encode the instrument catalog and session/domain model

**Files:**
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/InstrumentBrand.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/InstrumentModel.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/DelimiterStrategy.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/Session.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/MeasurementRecord.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/DiscoveredBluetoothDeviceItem.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/BondedBluetoothDeviceItem.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/domain/model/ExportFormat.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/instrument/InstrumentCatalog.kt`
- Create: `app/src/test/java/com/unforgettable/bluetoothcollector/data/instrument/InstrumentCatalogTest.kt`

- [ ] **Step 1: Extract brand/model options from the MIT App Inventor source**

Read the existing reference data from:

- `src/appinventor/ai_xiakele341/blue_tooth/Screen1.bky`

Use that to seed the brand/model catalog rather than inventing new values.

- [ ] **Step 2: Define the delimiter strategy enum**

```kotlin
enum class DelimiterStrategy {
    LINE_DELIMITED,
    WHITESPACE_TOKEN
}
```

- [ ] **Step 3: Define the immutable session model**

The session model must carry:

- session id
- started/updated timestamps
- immutable instrument metadata
- immutable target device identity
- delimiter strategy
- isCurrent flag

- [ ] **Step 4: Define the measurement record model**

The record model must carry:

- `id`
- sequence
- received timestamp
- raw payload
- parsed code/value
- copied instrument/device metadata for export traceability

- [ ] **Step 5: Build the instrument catalog source**

Use a focused structure, for example:

```kotlin
data class InstrumentModel(
    val brandId: String,
    val modelId: String,
    val displayName: String,
    val delimiterStrategy: DelimiterStrategy,
    val expectedTransport: String = "CLASSIC_BLUETOOTH_SPP",
)
```

Keep the catalog in one file for V1. Do not prematurely spread it across multiple data sources.

- [ ] **Step 6: Write a failing catalog sanity test**

Create:

`app/src/test/java/com/unforgettable/bluetoothcollector/data/instrument/InstrumentCatalogTest.kt`

Verify at least:

- every model belongs to a known brand
- every model has a delimiter strategy
- every model expects classic Bluetooth SPP transport

- [ ] **Step 7: Run the catalog test**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*InstrumentCatalogTest"
```

Expected:

- test passes after the catalog is implemented

- [ ] **Step 8: Commit the domain baseline**

```bash
git add app/src/main/java/com/unforgettable/bluetoothcollector/domain app/src/main/java/com/unforgettable/bluetoothcollector/data/instrument app/src/test/java/com/unforgettable/bluetoothcollector/data/instrument
git commit -m "feat: add collector domain models and instrument catalog"
```

### Task 3: Implement Bluetooth permission, discovery, pairing, and connect foundation

**Files:**
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/BluetoothSessionPolicy.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/BluetoothPermissionChecker.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/BluetoothDiscoveryManager.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/BondedDeviceManager.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/PairingRequestCoordinator.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/BluetoothConnectionManager.kt`
- Create: `app/src/test/java/com/unforgettable/bluetoothcollector/data/bluetooth/BluetoothSessionPolicyTest.kt`

- [ ] **Step 1: Write failing tests for the parser-free Bluetooth rules**

Create:

`app/src/test/java/com/unforgettable/bluetoothcollector/data/bluetooth/BluetoothSessionPolicyTest.kt`

Cover pure logic such as:

- discovery allowed only while disconnected
- connect requires bonded device
- reconnect to same device allowed for current session
- bond-loss on the selected saved device can re-enter pairing without forcing session clear
- clear current session refused while connected
- discovery unavailable while connected
- discovery can be blocked while connect-to-bonded remains available
- connect can be blocked while discovery remains available

Example:

```kotlin
@Test
fun `connect requires bonded device`() {
    val device = FakeBluetoothDevice(bondState = BOND_NONE)
    val decision = BluetoothSessionPolicy.canConnect(device)
    assertFalse(decision.allowed)
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*Bluetooth*"
```

Expected:

- failing tests because the Bluetooth policy and managers do not exist yet

- [ ] **Step 3: Implement the pure session policy**

`BluetoothSessionPolicy.kt` should own pure decisions so Android-specific managers stay smaller:

- when discovery is allowed
- when clear is allowed
- when connect is allowed
- when same-session re-pair is allowed
- how split permission states map to allowed actions

- [ ] **Step 4: Implement permission gating**

The checker must explicitly handle:

- Android 12+ discovery/connection permissions
- API 26-30 `ACCESS_FINE_LOCATION` requirement for discovery
- system Bluetooth disabled state
- split-permission behavior where discovery may be blocked while bonded-device connect remains available, and vice versa

- [ ] **Step 5: Implement discovery manager**

The discovery manager should expose:

- current discovery state
- discovered device list
- start discovery
- stop discovery
- auto-stop handling on system discovery-finished broadcast

Also enforce:

- cancel discovery before any connect attempt
- no discovery while connected

- [ ] **Step 6: Implement bonded-device manager**

This file owns:

- reload paired-device list
- expose current bonded devices
- refresh bonded-state after pairing succeeds
- allow lookup of the saved target device identity after app restore

- [ ] **Step 7: Implement pairing handoff coordinator**

Keep pairing simple:

- initiate Android system bond flow
- observe bond state change
- refresh bonded-device list after success
- allow same-session re-pair of the saved target device if its bond is lost

Do not build custom PIN-entry UX in V1.

- [ ] **Step 8: Implement connection manager**

Use classic Bluetooth socket connection with:

- SPP UUID
- bounded connect timeout
- explicit clean disconnected state on failure
- connected-but-not-receiving drain/discard behavior

- [ ] **Step 9: Run the unit tests again**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*BluetoothSessionPolicyTest"
```

Expected:

- Bluetooth policy tests pass

- [ ] **Step 10: Commit the Bluetooth foundation**

```bash
git add app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth app/src/test/java/com/unforgettable/bluetoothcollector/data/bluetooth
git commit -m "feat: add bluetooth discovery pairing and connection foundation"
```

## Chunk 2: Intake, Persistence, Export, UI, And Docs

### Task 4: Implement text-stream parsing and current-session persistence

**Files:**
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/TextStreamRecordParser.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/storage/AppDatabase.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/storage/SessionEntity.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/storage/MeasurementRecordEntity.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/storage/SessionDao.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/storage/MeasurementRecordDao.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/storage/CollectorRepository.kt`
- Test: `app/src/test/java/com/unforgettable/bluetoothcollector/data/bluetooth/TextStreamRecordParserTest.kt`
- Test: `app/src/test/java/com/unforgettable/bluetoothcollector/data/storage/CollectorRepositoryTest.kt`

- [ ] **Step 1: Write failing parser tests**

Cover:

- line-delimited splitting
- whitespace-token splitting
- partial-fragment buffering
- stop-receive dropping incomplete fragment
- overflow trimming behavior
- cleaned text preserved as raw payload when parsing does not produce code/value
- control-character garbage is dropped
- light parsing extracts `parsedCode` and `parsedValue` when the cleaned record shape allows it

Example:

```kotlin
@Test
fun `line delimited parser keeps trailing fragment buffered`() {
    val parser = TextStreamRecordParser()
    val result = parser.accept("01 123.456\n02 234", DelimiterStrategy.LINE_DELIMITED)
    assertThat(result.completed).containsExactly("01 123.456")
    assertThat(result.remainingBuffer).isEqualTo("02 234")
}
```

- [ ] **Step 2: Run parser tests to verify they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*TextStreamRecordParserTest"
```

Expected:

- tests fail before implementation

- [ ] **Step 3: Write failing repository/session tests**

Create:

`app/src/test/java/com/unforgettable/bluetoothcollector/data/storage/CollectorRepositoryTest.kt`

Cover at least:

- create current session when absent
- append record to current session
- restore current session after simulated app restart
- clear current session deletes session records
- same-session continuation allowed for same instrument and same device
- repository remains persistence-focused and does not own transport-state rules

- [ ] **Step 4: Run repository tests to verify they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*CollectorRepositoryTest"
```

Expected:

- repository tests fail before implementation

- [ ] **Step 5: Implement the parser**

The parser must match the spec exactly:

- runtime does not auto-detect delimiter strategy
- `LINE_DELIMITED` only splits on `\r`, `\n`, `\r\n`
- `WHITESPACE_TOKEN` splits on whitespace tokens
- valid cleaned text is preserved in `rawPayload`
- light parsing fills `parsedCode` / `parsedValue` when possible
- control-character garbage is dropped
- bad fragments do not kill the stream
- incomplete fragment is held until completed
- incomplete fragment is dropped on stop
- overflow trims old undecodable data and keeps newest tail

- [ ] **Step 6: Define Room entities and DAOs**

Create two tables:

- `sessions`
- `measurement_records`

Keep one current session via `isCurrent = true` rather than inventing multiple V1 history concepts.

- [ ] **Step 7: Implement repository rules**

Repository responsibilities:

- create current session when needed
- append records to current session
- restore current session on app launch
- allow same-session continuation for the same device and model

Do not put live Bluetooth transport decisions in the repository. Rules such as “clear only while disconnected” stay in `BluetoothSessionPolicy` and the ViewModel.

- [ ] **Step 8: Run parser and storage unit tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*TextStreamRecordParserTest" --tests "*CollectorRepositoryTest"
```

Expected:

- parsing and repository tests pass

- [ ] **Step 9: Commit intake and persistence**

```bash
git add app/src/main/java/com/unforgettable/bluetoothcollector/data/bluetooth/TextStreamRecordParser.kt app/src/main/java/com/unforgettable/bluetoothcollector/data/storage app/src/test/java/com/unforgettable/bluetoothcollector/data/bluetooth app/src/test/java/com/unforgettable/bluetoothcollector/data/storage
git commit -m "feat: add stream parsing and current session persistence"
```

### Task 5: Implement CSV/TXT export and Android sharing

**Files:**
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/export/CsvExportWriter.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/export/TxtExportWriter.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/export/ExportFileNamer.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/data/share/ShareLauncher.kt`
- Test: `app/src/test/java/com/unforgettable/bluetoothcollector/data/export/CsvExportWriterTest.kt`
- Test: `app/src/test/java/com/unforgettable/bluetoothcollector/data/export/TxtExportWriterTest.kt`

- [ ] **Step 1: Write failing export tests**

Cover:

- UTF-8 output
- CSV quoting
- CRLF line endings
- timestamped unique file names
- TXT header includes session/device metadata

Example:

```kotlin
@Test
fun `csv writer quotes payload containing comma`() {
    val csv = CsvExportWriter().write(records = listOf(sampleRecord(rawPayload = "A,B")))
    assertTrue(csv.contains("\"A,B\""))
}
```

- [ ] **Step 2: Run export tests to verify they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*CsvExportWriterTest" --tests "*TxtExportWriterTest"
```

Expected:

- tests fail before implementation

- [ ] **Step 3: Implement CSV writer**

Must match the spec:

- include required columns
- use ISO 8601 timestamps with offset
- use UTF-8
- use CRLF
- never overwrite an existing export file

- [ ] **Step 4: Implement TXT writer**

Must include:

- session start time
- instrument brand/model
- Bluetooth device name/address
- export time
- raw payload rows in receive order

- [ ] **Step 5: Implement sharing wrapper**

Keep sharing simple and explicit:

- exported file generated in app-managed storage
- `FileProvider` uri
- `ACTION_SEND`

- [ ] **Step 6: Run export tests again**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*CsvExportWriterTest" --tests "*TxtExportWriterTest"
```

Expected:

- export tests pass

- [ ] **Step 7: Commit export and share**

```bash
git add app/src/main/java/com/unforgettable/bluetoothcollector/data/export app/src/main/java/com/unforgettable/bluetoothcollector/data/share app/src/test
git commit -m "feat: add export and share pipeline"
```

### Task 6: Build the single-screen UI and state machine

**Files:**
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorUiState.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModel.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorRoute.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorScreen.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/collector/ExportFormatDialog.kt`
- Create: `app/src/main/java/com/unforgettable/bluetoothcollector/ui/theme/Theme.kt`
- Test: `app/src/test/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorViewModelTest.kt`
- Test: `app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorScreenTest.kt`
- Test: `app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorWorkflowTest.kt`
- Modify: `app/src/main/java/com/unforgettable/bluetoothcollector/MainActivity.kt`

- [ ] **Step 1: Write failing ViewModel tests**

Cover:

- restore current session on launch
- discovery/connection mutual exclusion
- connect requires bonded device
- start receive creates session if absent
- clear only allowed while disconnected
- app restart restores current session data but not connection/receiving state
- export dialog opens with CSV/TXT choices
- share event is emitted after export preparation

- [ ] **Step 2: Run ViewModel tests to verify they fail**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "*CollectorViewModelTest"
```

Expected:

- ViewModel tests fail before implementation

- [ ] **Step 3: Implement the UI state**

State must include at least:

- selected brand/model
- discovered devices
- bonded devices
- selected target device
- discovery state
- connection state
- receive state
- permission state
- current session preview rows
- received count
- export dialog visibility
- error/info message state

- [ ] **Step 4: Implement the ViewModel**

The ViewModel must orchestrate:

- permission requests and derived UI state
- discovery start/stop
- pairing handoff
- bonded-device connection
- start/stop receive
- current session restore and clear
- export request and share handoff

- [ ] **Step 5: Implement the Compose screen**

The UI must render:

- left instrument selection column
- right Bluetooth area with nearby devices and paired devices
- start/stop/clear/export controls
- connection and receive status
- ordered preview list

Keep it on one screen. Do not reintroduce a multi-screen workflow.

- [ ] **Step 6: Add Compose UI tests for critical UI contracts**

Create:

- `app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorScreenTest.kt`
- `app/src/androidTest/java/com/unforgettable/bluetoothcollector/ui/collector/CollectorWorkflowTest.kt`

Validate at least:

- export dialog shows `CSV` and `TXT`
- nearby device section and paired device section both render
- restored current session preview is shown while connection state remains disconnected
- export action produces the expected UI event path for sharing

- [ ] **Step 7: Run unit and UI tests**

Run:

```bash
./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest
```

Expected:

- unit tests pass
- instrumented UI tests pass on a connected emulator/device

- [ ] **Step 8: Commit the UI integration**

```bash
git add app/src/main/java/com/unforgettable/bluetoothcollector/ui app/src/main/java/com/unforgettable/bluetoothcollector/MainActivity.kt app/src/androidTest app/src/test
git commit -m "feat: add single-screen collector ui"
```

### Task 7: Run release-critical manual acceptance checks

**Files:**
- Test: app runtime behavior on at least one Android 12+ or newer device/emulator

- [ ] **Step 1: Verify nearby discovery and pairing**

Manual path:

1. choose instrument brand/model
2. start nearby discovery
3. observe discovered devices
4. pair one unbonded device through the Android system pairing flow
5. confirm it appears in the bonded-device list

Expected:

- discovery starts and stops cleanly
- pairing succeeds or failure state is visible and recoverable
- the bonded list updates after successful pairing

- [ ] **Step 2: Verify connection and receive lifecycle**

Manual path:

1. connect to a bonded device
2. start receive
3. observe preview rows and receive count
4. stop receive
5. disconnect
6. relaunch app

Expected:

- records appear in order
- current session survives relaunch
- relaunch restores data only, not active connection or receiving state

- [ ] **Step 3: Verify export and share**

Manual path:

1. keep the app in the post-relaunch disconnected state verified in Step 2
2. export current session as CSV
3. export current session as TXT
4. trigger Android share sheet for each export

Expected:

- both files are generated from the restored current session
- CSV and TXT match the spec format
- share sheet opens for both exports

- [ ] **Step 4: Record verification outcome**

Capture:

- tested Android version
- tested device/emulator
- any known gaps that remain after manual validation

### Task 8: Update repository docs and legacy positioning

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Rewrite the README around the native app**

Cover:

- repo now contains legacy MIT App Inventor reference plus native Android project
- V1 workflow
- supported Bluetooth flow: discovery, pairing, bonded-device connect, text-stream intake
- export/share behavior

- [ ] **Step 2: Add a short migration note**

State clearly:

- old App Inventor sources remain as historical reference
- new implementation path is native Android Studio

- [ ] **Step 3: Verify the main build and docs references**

Run:

```bash
./gradlew :app:assembleDebug
rg -n "App Inventor|native Android|Bluetooth" README.md
```

Expected:

- app assembles
- README matches the current project direction

- [ ] **Step 4: Commit docs cleanup**

```bash
git add README.md
git commit -m "docs: describe native android collector workflow"
```

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-03-30-native-android-total-station-collector.md`. Ready to execute?
