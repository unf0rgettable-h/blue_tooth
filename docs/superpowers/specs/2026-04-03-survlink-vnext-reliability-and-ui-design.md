# SurvLink vNext Reliability And UI Design

## Background

The current repository has already completed the native Android rewrite, but field testing exposed three critical problems that block the next release:

1. Real-time receive regressed after later iterations. In earlier versions, tapping `开始接收` and storing a measurement on the instrument immediately produced a new preview row on the phone. That behavior no longer works.
2. Whole-file import works for TS09 through the current `导入存储数据` entry, but TS60 fails at the instrument side when the operator chooses Bluetooth as the RS232 port and exports `ASCⅡ数据（格式文件）`. This is not a pure parser issue; the instrument reports transport/protocol/device-readiness failure before any usable file reaches the app.
3. The current single-screen layout is overpacked. On some phones, buttons and panels are partially clipped. Bluetooth management, receive controls, import controls, preview, and file actions are crowded into one surface.

This version must ship as a single release that addresses reliability and UI structure together. The implementation may begin as a compatibility-focused refactor, but if root-cause investigation proves the current structure is too entangled to safely evolve, the work must escalate into a structural rewrite inside the same release branch rather than layering more patches on top.

## Product Goal

Ship a new SurvLink release that:

- restores reliable real-time measurement receive for supported Leica workflows, including TS60
- preserves working TS09 file import
- introduces a model-aware batch import strategy instead of assuming TS09 and TS60 share the same whole-file export path
- replaces the current crowded single-screen UI with a two-page bottom-navigation structure that fits smaller phones and separates Bluetooth management from data operations
- makes import/file status visible at all times in the data workflow instead of only after an import succeeds

## Confirmed Field Facts

These facts are treated as design constraints, not hypotheses:

- TS09 whole-file import currently works through the app's `导入存储数据` action.
- TS60 whole-file import has never worked in the field through that same path.
- TS60 real-time receive worked in an earlier native release and therefore is considered a regression, not an unsupported capability.
- The operator wants one release that addresses the import problem, the real-time regression, and the UI restructuring together.
- The operator accepts a model-specific import experience for TS60 as long as the app still presents one unified `导入存储数据` entry point and then guides the user appropriately by instrument/model.

## External Protocol Research Findings

The design must incorporate the following research-backed constraints:

- Leica FlexLine TS02/TS06/TS09 documentation explicitly exposes communication parameters with selectable `Port` values including `RS232` and `Bluetooth`, and the manual includes dedicated sections for `Exporting Data`, `Importing Data`, and `Working with Bluetooth`.
- Leica Captivate documentation explicitly documents `Export ASCII (Format File)` as a custom-format export workflow. It requires:
  - at least one format file already loaded into internal memory
  - selection of export target device
  - selection of export folder
  - use of the currently configured `RS232 interface` when the export target is `RS232`
- Leica Captivate connection documentation shows serial-style parameters for cable/Bluetooth transport such as baud rate, parity, data bits, stop bit, and flow control. Transport configuration is therefore part of the functional contract.
- Leica Captivate also documents a separate `GeoCOM Connection` mode for third-party devices. Community Leica communication libraries such as GeoComPy and PyGeoCOM treat `GeoCOM` and `GSI Online` as different protocol families rather than one generic text stream.
- GeoComPy documentation states that Leica Bluetooth communication uses Bluetooth Classic Serial Port Profile (SPP), effectively emulating a serial line, and warns that the instrument-side GeoCOM interface may need to be explicitly switched to the Bluetooth device and matched on speed/parity parameters before communication works correctly.
- GeoComPy further documents that GeoCOM supports transaction IDs and checksums while GSI Online does not. This means not every successful Leica data path has the same reliability guarantees.

### Design Implication Of Research

The release must not assume that:

- TS09 and TS60 share one identical export path
- live receive and whole-file export ride on the same protocol layer
- Bluetooth transport alone implies the same application-layer behavior across FlexLine and Captivate

Instead, the design must explicitly treat TS09/FlexLine and TS60/Captivate as separate profile families with shared UI entry points but potentially different supported workflows, parameter requirements, and fallback behavior.

## In Scope

### Functional

- restore the live receive path so that a connected instrument measurement can immediately create a preview row again
- retain copyable preview rows
- keep TS09 batch import working
- add model-specific import profiles so the app no longer assumes all instruments can use the same batch-import workflow
- add explicit disconnect control to the Bluetooth page
- make import file status persistently visible in the data page even when no imported file exists yet

### UX / Information Architecture

- replace the current single-screen collector UI with bottom navigation and exactly two top-level pages:
  - `蓝牙`
  - `数据`
- move brand/model selection to the Bluetooth page
- keep all connect/disconnect/pair/discovery operations on the Bluetooth page
- move `开始接收`, `停止接收`, `导入存储数据`, preview, export, and file visibility to the Data page
- redesign layout density so both pages remain usable on smaller phones without clipped controls

### Reliability / Maintainability

- separate Bluetooth management, live receive, and batch import into distinct runtime paths with clear ownership
- repair drift in the test suite so tests match the actual `CollectorScreen` API and become meaningful regression protection again

## Out of Scope

- add-constant calculation or repeatability calculation
- legacy App Inventor feature restoration beyond using old project data as catalog/protocol reference
- background service collection
- cloud sync or remote storage
- a third top-level page such as dashboard/settings in this release
- deep brand-specific protocol parsing beyond what is needed to stabilize current supported workflows

## Navigation Model

The app will use bottom navigation with exactly two destinations:

1. `蓝牙`
2. `数据`

This is the default design direction because it best addresses the field complaints about clipped controls and overpacked layout. It also matches the operator's preference after visual review.

### Bluetooth Page Responsibilities

The Bluetooth page is the device and session setup surface. It must contain:

- Bluetooth enabled / permission / connection status summary
- brand selector
- model selector
- nearby devices list
- paired devices list
- search start / search stop
- pair action for nearby devices where applicable
- connect action
- disconnect action
- selected target device visibility

Setup rules on this page are mandatory:

- nearby unbonded devices may be paired from the nearby list
- connection attempts are made only against bonded devices
- direct connect to an unbonded device is invalid for this release
- live receive and batch import are both unavailable until the selected target device is bonded and connected

The Bluetooth page must not contain:

- `开始接收`
- `停止接收`
- `导入存储数据`
- preview records
- export/share controls

Reason: Bluetooth management and data operations caused layout crowding and made it harder to reason about the runtime state.

### Data Page Responsibilities

The Data page is the collection and evidence surface. It must contain:

- current session summary
- live receive controls:
  - `开始接收`
  - `停止接收`
- batch import controls:
  - `导入存储数据`
  - model-aware import guidance / instructions
- import file panel:
  - always visible
  - empty state when no imported file exists yet
  - populated state when a file has been imported
  - share/save affordances when a file exists
- real-time / offline preview list
- export/share controls
- save-to-local control

The unified `导入存储数据` entry point remains visible on the Data page for all supported models, but its runtime behavior depends on the selected import profile:

- `SUPPORTED`
  - tapping the entry starts the import workflow after showing any required guidance
- `GUIDANCE_ONLY`
  - tapping the entry opens a guidance panel or sheet and does not enter a receive state
- `UNSUPPORTED`
  - tapping the entry opens an explicit unsupported message describing the limitation for this release and does not enter a receive state

The unified entry therefore stays visible for all profiled models, but only `SUPPORTED` profiles may transition into an active import state. For `GUIDANCE_ONLY` and `UNSUPPORTED`, the visible CTA is a guidance action, not an import-start action.

#### Import Control State Table

| Profile verdict | Button label | Enabled | Tap result | Lane transition |
| --- | --- | --- | --- | --- |
| `SUPPORTED` | `导入存储数据` | Yes | starts import workflow | may enter `awaitingImportData` |
| `GUIDANCE_ONLY` | `查看导入说明` | Yes | opens guidance sheet/panel | no import-state transition |
| `UNSUPPORTED` | `查看限制说明` | Yes | opens unsupported/limitation sheet | no import-state transition |

### Action Boundary Rules

The Data page contains two different artifact types and they must not be conflated:

1. `Current session preview/records`
   - actions:
     - `导出并分享`
     - `保存到本地` for exported session output
2. `Imported file artifact`
   - actions:
     - `分享文件`
     - `保存到本地` for the imported raw file

The UI must make it obvious which action targets which artifact. The imported-file panel's actions only apply to the imported file. Session export/share actions only apply to persisted preview/session records.

### Layout Rules

- each page may scroll, but no primary action row may require horizontal scrolling
- critical action groups must wrap or stack on narrower screens instead of clipping
- state summary must remain visible near the top of each page
- imported-file visibility may not depend on the user first executing a successful import

### Bluetooth Page Blocking/Error Recovery

The Bluetooth page must provide deterministic, user-understandable behavior for these blocking/error cases:

- permission denied
- Bluetooth adapter powered off
- connect failure

Required recovery behavior:

- if permissions are missing, the page must clearly identify what permission is missing and provide a visible retry/request path
- if Bluetooth is powered off, the page must show that state and make reconnect/discovery unavailable until Bluetooth returns
- if connect fails, the page must stay navigable, keep the selected target visible, and allow the user to retry, choose another bonded device, or return to pairing/discovery

## Runtime Architecture

The current implementation exposes too much coupling between connection state, receive state, import state, and page rendering. The new design requires three explicit runtime lanes:

1. `Bluetooth management lane`
2. `Live receive lane`
3. `Batch import lane`

These lanes may coordinate through shared app state, but they must not share a single ambiguous "receiving/importing does everything" flow.

### Transport Ownership Contract

One transport-owning unit must exclusively own:

- socket lifecycle
- input stream lifecycle
- connection teardown

The Bluetooth management lane owns connection establishment and teardown, but it must expose transport access to the live receive and batch import lanes through an explicit handoff contract.

Minimum transport contract:

- only one consumer lane may hold active read access at a time
- live receive and batch import each acquire exclusive read access before starting blocking reads
- stopping, cancelling, disconnecting, or link loss releases that read access deterministically
- reconnect always creates a fresh transport session instead of reusing stale read ownership
- no lane may continue reading after disconnect, cancel, or ownership revocation

#### Transport Handoff Events

| Event | Requested by | Enforced by | Result |
| --- | --- | --- | --- |
| `acquireLiveReceiveReadAccess` | live receive lane | transport-owning unit | live receive becomes sole read owner |
| `acquireBatchImportReadAccess` | batch import lane | transport-owning unit | batch import becomes sole read owner |
| `releaseReadAccess` | active consumer lane | transport-owning unit | connection returns to connected-idle state |
| `revokeReadAccessOnDisconnect` | Bluetooth management lane | transport-owning unit | read access cleared, consumer lane terminated |
| `revokeReadAccessOnCancel` | batch import lane or live receive lane | transport-owning unit | read access cleared, requesting lane terminated |
| `resetTransportSessionOnReconnect` | Bluetooth management lane | transport-owning unit | stale ownership discarded, fresh transport session created |

## Shared State Ownership And Arbitration

The release must define one app-facing state owner that publishes the UI model consumed by the two pages. That owner may be one root coordinator/view model or a small shell above multiple page/view coordinators, but its responsibilities must be explicit:

- subscribe to the Bluetooth management lane
- subscribe to the live receive lane
- subscribe to the batch import lane
- publish page-safe UI state
- arbitrate mutually exclusive actions

The mandatory arbitration rules are:

- live receive and batch import are mutually exclusive
- starting live receive while import is active must be refused with visible status
- starting batch import while live receive is active must be refused with visible status
- disconnect or physical link loss immediately terminates any active live receive or batch import state
- a terminated receive/import flow must leave the app in a deterministic disconnected-or-connected-idle state instead of a half-running state
- brand/model/selected device mutation is blocked while connected, while live receive is active, while batch import is active, or while a current session exists
- to change brand/model/device for a different workflow, the user must first stop any active receive/import, disconnect if needed, and clear the current session

The root app-facing state must expose at least:

- connection state
- selected brand/model/device metadata
- whether live receive is active
- whether batch import is active
- current session summary
- imported-file summary / empty state
- current status/error message

The arbitration-visible statuses must include:

- live receive refused because import is active
- batch import refused because live receive is active

### Current Session Lifecycle

The app uses exactly one persisted current session.

- a current session is created when live receive first starts successfully
- the current session survives app restart and must be restored on launch into a disconnected, not-receiving state
- restoring a current session restores:
  - brand
  - model
  - selected device identity
  - persisted preview/session records
- restoring a current session does not restore an active transport connection or active receive/import state
- the user must have an explicit `清空当前会话` action in the Data page workflow
- clearing the current session deletes the persisted current session and its persisted preview/session records
- clearing the current session unlocks brand/model/device mutation

#### Imported Artifact Lifecycle

- a successful imported raw file artifact is independent from the current session
- a successful imported raw file artifact survives app restart and is restored into the imported-file panel
- `清空当前会话` clears only the current session and preview/session records
- `清空当前会话` does not delete the last successful imported raw file artifact
- a newer successful import replaces the previously shown imported raw file artifact
- a failed or cancelled import attempt preserves the last successful imported raw file artifact

### 1. Bluetooth Management Lane

Responsible for:

- permissions
- adapter state
- discovery
- bonded device refresh
- pairing requests
- connect
- disconnect
- physical link lost events

This lane owns the canonical connection state:

- `DISCONNECTED`
- `CONNECTING`
- `CONNECTED`

It does not own live parsing or file import behavior.

#### Bluetooth Management Interface Requirements

Inputs:

- refresh permissions
- start discovery
- stop discovery
- refresh bonded devices
- pair selected device
- connect selected device
- disconnect

Outputs:

- connection state changes
- discovery state changes
- paired/nearby device updates
- adapter powered off
- physical link lost
- permission state

Connection contract:

- nearby-device discovery may surface both bonded and unbonded devices
- `配对` is the only valid path from unbonded nearby device to connectable target
- `连接` is valid only for a bonded selected target device
- live receive and batch import lanes may assume that any active transport session started from the Bluetooth lane is already bonded and connected

### 2. Live Receive Lane

Responsible for:

- start live receive
- stop live receive
- blocking byte reads for live streaming
- text parsing
- persistence of parsed records
- preview updates

It must have its own mode/state separate from import:

- `idle`
- `liveReceiving`

The key requirement is restoring the previously working behavior:

`开始接收 -> instrument stores measurement -> app appends preview row`

Preview text selection/copy support must remain strictly a UI concern and must not influence the read loop, parser, or persistence semantics.

#### Live Receive Interface Requirements

Inputs:

- start live receive
- stop live receive
- disconnect/link-lost notification

Outputs:

- live receive state
- newly appended preview/session records
- visible failure reason when start is refused
- visible failure reason when link is lost mid-stream

#### Live Receive Preconditions

`开始接收` may be enabled only when one of the following is true:

- an existing current session is already restored and the app is connected to that session's device
- or all required session metadata is present and valid:
  - selected brand
  - selected model
  - selected bonded target device
  - active Bluetooth connection to that device

If any required metadata is missing, the app must:

- block live receive start
- show a visible status/error message
- make the recovery path obvious: the user returns to the Bluetooth page, completes selection/connection, and then retries

#### Selection Mutation Rules

The same metadata rules also apply to import profile selection because import profiles are derived from the selected brand/model/device context:

- if no current session exists and the app is disconnected, brand/model/device may be edited freely
- if a current session exists, the selected brand/model/device are treated as session metadata and remain locked until the current session is cleared
- if the user attempts to change brand/model/device while locked, the app must refuse the action with visible guidance rather than partially updating state

### 3. Batch Import Lane

Responsible for:

- start import session
- show model-specific import guidance
- raw byte accumulation
- silence-based import completion
- imported-file detection and persistence
- imported-file panel updates

It must have its own mode/state separate from live receive:

- `idle`
- `awaitingImportData`
- `importReceiving`
- `importCompleted`
- `importFailed`

This lane must not reuse the live receive parser/persistence path as though a whole file were merely a bigger live stream.

#### Batch Import Interface Requirements

Inputs:

- start import
- stop/cancel import
- disconnect/link-lost notification
- import profile selection from current model

Outputs:

- import state
- visible operator guidance
- imported-file panel state
- clear import failure reason

Cancel contract:

- user-triggered cancel is distinct from disconnect/link-loss interruption
- cancelling an import from either `awaitingImportData` or `importReceiving` must transition the import lane back to `idle`
- cancelling must show a visible `import_cancelled`-style status
- cancelling must discard uncommitted partial bytes and must not surface a false imported-file success artifact
- cancelling must preserve the last successfully imported file, if one already existed before the cancelled attempt

#### Batch Import Preconditions

`导入存储数据` may transition into an active import state only when all required metadata is present and valid:

- selected brand
- selected model
- selected bonded target device
- active Bluetooth connection to that device
- import profile verdict is `SUPPORTED`

If any precondition is missing, the app must:

- block import start
- show a visible refusal status
- make the recovery path obvious: return to the Bluetooth page, complete pairing/selection/connection, then retry

If the import profile verdict is `GUIDANCE_ONLY` or `UNSUPPORTED`, the app must not enter `awaitingImportData` or `importReceiving`.

#### Awaiting Import Data Timeout Policy

The release will not auto-fail before the first byte arrives. In `awaitingImportData`, the app:

- remains in the waiting state until bytes arrive, the user cancels, or the link is disconnected
- must display a waiting status so the operator understands the app is expecting export to begin from the instrument
- must not silently time out into failure without operator action

## Import Profiles

The app will keep a unified user-facing action named `导入存储数据`, but internally it must route through an import profile chosen from the selected instrument model.

### Import Profile Contract

Each supported model/import profile must define:

- supported batch-import capability: yes/no
- user guidance text shown before import
- receive strategy
- completion condition
- post-import file handling
- unsupported/fallback message when a known instrument-side export mode cannot work

The profile contract must return at least:

- support verdict:
  - `SUPPORTED`
  - `GUIDANCE_ONLY`
  - `UNSUPPORTED`
- operator guidance text
- receive strategy:
  - raw whole-file receive
  - no receive, guidance only
- completion rule
- file-panel labeling and actions
- unsupported export mode text where applicable

The profile must also define how the Data page labels the unified import entry and guidance area so that a user can understand whether the current model/path is actionable, guidance-only, or unsupported without guessing.

For unknown or unmapped brand/model combinations, the registry must fall back to `UNSUPPORTED` with an explicit message that the model is not profiled for batch import in this release.

Behavioral meaning of the support verdict is fixed:

- `SUPPORTED`
  - the app exposes an active import workflow and expects to receive bytes
- `GUIDANCE_ONLY`
  - the app does not attempt batch receive for that path, but shows explicit operator guidance describing the known limitation and the best supported workflow available in this release, which may be live receive only
- `UNSUPPORTED`
  - the app states that no supported batch-import path exists for that model/path in this release and does not present an import-start action for that unsupported subpath

### TS09 Profile

The current working TS09 behavior becomes the baseline profile:

- preserve existing successful whole-file receive behavior
- keep silence-based completion if it still matches observed success
- do not regress operator workflow

For the baseline whole-file import path, the post-first-byte stall policy is:

- after the first bytes arrive, silence is treated as end-of-transmission completion
- the default completion threshold for this release is `3 seconds` of silence, matching the current successful path unless investigation proves a better threshold is required
- silence after bytes have started is not treated as failure by default
- failure after bytes have started is reserved for explicit error conditions such as disconnect, user cancel, size guard breach, or unrecoverable write/parse handling

Completion validity rules for a successful imported artifact:

- zero bytes is not a success artifact
- bytes rejected by the oversize guard is not a success artifact
- bytes that fail final file write is not a success artifact
- bytes written successfully produce the imported artifact even if later higher-level interpretation remains limited

### TS60 Profile

TS60 must no longer be treated as "same as TS09 unless proven otherwise". The implementation must support a TS60-specific profile.

The product requirement for this release is:

- investigate and ship at least one field-valid whole-file import workflow for TS60 if such a workflow can be verified through the instrument and app together
- if the currently attempted `ASCⅡ数据（格式文件）` over Bluetooth-as-RS232 path is confirmed unsupported or fundamentally incompatible, the app must explicitly say so in TS60 guidance instead of pretending the shared TS09 import path applies
- the TS60 profile must clearly separate:
  - supported workflow
  - unsupported workflow
  - operator instructions

For TS60 specifically, the investigation must consider at least these candidate explanation buckets before implementation chooses a final path:

- transport mismatch
- RS232/Bluetooth interface configuration mismatch
- format-file dependency mismatch
- Captivate export-mode mismatch
- protocol-family mismatch between the app's current assumptions and the instrument's configured output mode

This means the release is allowed to diverge by model as long as the user-facing entry remains coherent and the operator receives explicit model-correct guidance.

#### TS60 Investigation Decision Gate

The implementation plan must stop pursuing a supported TS60 batch-import workflow for this release and pivot to `GUIDANCE_ONLY` or `UNSUPPORTED` when the planned investigation slice completes without evidence of a reproducible supported path. Evidence required to keep pursuing support:

- a reproducible operator workflow
- signs that the instrument begins transmitting bytes toward the app or external receiver
- no documented incompatibility that rules the path out for this release

Without that evidence, the release must record the limitation explicitly rather than leaving the scope open-ended.

## Root-Cause Investigation Requirements

Before any production fix is merged, the implementation must complete root-cause investigation for both reliability issues.

### Real-Time Receive Regression

The implementation must compare the known-good behavior from earlier native releases against current code and identify the actual regression source. Possibilities include, but are not limited to:

- receive-loop state handling
- idle-drain behavior
- parser buffering semantics
- connection lifecycle changes
- UI-to-state event wiring
- test drift masking the regression

No fix is acceptable unless it is tied to an investigated cause rather than a guess.

The release must capture a live-receive verification record for the repaired path, including:

- tested model/device path
- operator steps
- observed app behavior
- evidence that preview rows appear again in real time

For TS60, this verification is mandatory because the regression was observed specifically on a device family that previously worked.

### TS60 Batch Import Failure

The implementation must treat the current field error as a compatibility/path-selection problem until evidence proves otherwise.

The investigation must determine:

- whether the failure occurs before the app receives any bytes
- whether the currently chosen TS60 export mode is incompatible with the app's expected transport contract
- whether TS60 has an alternate whole-file export workflow that can be supported in this release
- what user-facing guidance is required for supported vs unsupported TS60 export paths

## Error Handling And Status Requirements

The new version must improve status clarity in two places:

### Live Receive

- clearly distinguish "connected but not receiving" from "actively receiving"
- show a visible error when the physical link is lost
- show a visible error when receive cannot start because session metadata is incomplete

### Batch Import

- clearly distinguish:
  - waiting for operator to export from instrument
  - actively receiving import bytes
  - import completed
  - import failed
  - unsupported TS60 export mode / unsupported model path
- keep the import file panel visible even when empty so the user understands where successful files will appear

If disconnect or physical link loss occurs during either:

- `awaitingImportData`
- `importReceiving`

then the batch import lane must:

- terminate the import state immediately
- clear any transient "receiving" state
- avoid presenting a partially received file as successful import output
- preserve only artifacts that have already reached a valid completed state
- surface a visible interruption failure state

### Required Observable Statuses

The following statuses are mandatory release behavior, not implementation detail:

- connected, not receiving
- actively live receiving
- live receive blocked because session metadata is incomplete
- physical link lost
- import waiting for operator export action
- import actively receiving bytes
- import cancelled by user
- import failed
- import completed
- import interrupted by disconnect/link loss
- unsupported TS60 export mode / guidance-only TS60 path

For TS60, live receive restoration is mandatory in this release. TS60 batch import may ship as either:

- a supported whole-file workflow
- or an explicit guidance-only / unsupported result for the tested TS60 export path

but the app may not ship while silently failing to distinguish those outcomes.

### Imported File Panel State Rules

The imported-file panel must have deterministic behavior across retries:

- before any successful import, show empty state
- after a successful import, show the last successful imported file
- during a new import attempt, keep the last successful imported file visible as the current known artifact unless and until a new import completes successfully
- if a new import attempt fails or is cancelled, do not replace the previous successful file artifact with a false or partial artifact
- if no prior successful import exists and the current attempt fails or is cancelled, return to empty-state artifact display plus visible failure/cancel status

## Component Boundary Requirements

The implementation must result in units that are independently understandable and testable.

Minimum boundary expectations:

- navigation/page shell
- Bluetooth page UI
- Data page UI
- Bluetooth orchestration/controller
- live receive coordinator
- batch import coordinator
- import profile registry / model mapping
- repository/persistence

Repository/persistence contract must distinguish two artifact classes:

- `current session data`
  - persisted session metadata
  - persisted preview/measurement records
- `imported raw file artifact`
  - imported file metadata
  - imported file storage location / share/save information

The plan may implement both classes in one persistence layer or in separate helpers, but their contracts must remain distinct so that session export actions cannot accidentally target imported files and vice versa.

The app may still use one `CollectorViewModel` if the resulting code remains clear, but if the current file cannot support these boundaries without continued confusion, the implementation must split it into smaller focused units rather than preserving a too-large coordinator out of inertia.

## Testing Requirements

This release is not ready without explicit regression coverage.

### Unit Tests

Add or update tests for:

- import profile selection by instrument model
- live receive state transitions
- batch import state transitions
- import empty-state visibility logic
- parser behavior where relevant
- imported-file format detection where relevant

### ViewModel Tests

Cover at least:

- Bluetooth page state vs Data page state ownership
- live receive start/stop behavior
- batch import start/complete/failure behavior
- batch import cancel behavior
- disconnect behavior while connected or receiving
- disconnect or link-loss behavior while import is waiting or actively receiving
- empty imported-file panel visibility rules
- TS09 vs TS60 import guidance/profile branching
- last successful imported-file preservation across failed/cancelled retries
- mutual-exclusion refusal statuses for live receive vs batch import

### Compose / UI Tests

Cover at least:

- bottom navigation renders both pages
- Bluetooth page shows connect and disconnect controls
- Data page shows start receive and import controls
- imported-file panel exists even when no file has been imported yet
- preview list still renders copyable text
- required observable statuses appear in the correct page context
- imported-file actions are visually separated from session export actions
- Bluetooth page shows clear recovery affordances for permission denied / adapter off / connect failure states

### Test Suite Repair

The current drift in `androidTest` signatures must be fixed as part of this work. A test suite that no longer matches the real `CollectorScreen` contract is not acceptable regression coverage.

### Layout Verification Matrix

Criterion 9 is satisfied only if the redesigned pages are verified against at least these representative phone widths:

- compact width around `360dp`
- common modern width around `393dp`
- wider handset width around `412dp`

Verification may be automated, manual, or both, but it must confirm that primary controls are not clipped and action rows remain usable.

## Acceptance Criteria

This design is considered successfully implemented only if all of the following are true:

1. The app uses bottom navigation with exactly two top-level pages: `蓝牙` and `数据`.
2. Brand/model selection is on the Bluetooth page.
3. Connect and disconnect controls are present and usable from the Bluetooth page.
4. `开始接收` is on the Data page and live preview updates again during real-time measurement storage, including verified TS60 live receive restoration.
5. The imported-file area is visible on the Data page even before any import succeeds.
6. TS09 whole-file import still works.
7. TS60 batch import no longer relies on the false assumption that TS09 and TS60 share one identical path.
8. The release contains either:
   - a verified TS60 whole-file import workflow, or
   - explicit TS60 guidance that correctly identifies unsupported export modes and, if no supported batch-import path exists in this release, states that limitation clearly and routes the operator toward the best supported alternative workflow available in this release.
9. UI no longer clips primary controls on narrower phones in the tested layouts.
10. Tests are updated to match the actual UI contract and provide regression protection for the new structure.
11. The app visibly distinguishes `connected but idle` from `actively receiving`.
12. The app visibly reports live receive blocked by incomplete session metadata and gives the user a recovery path.
13. The app visibly reports import waiting, import receiving, import interrupted, and import completed states.
14. The app visibly reports import failed, import cancelled, and mutually-exclusive start refusal states.
15. The Bluetooth page visibly handles permission denied, adapter off, and connect failure states with clear recovery paths.

### TS60 Verification Evidence Requirement

Acceptance criterion 8 is satisfied only if the release work captures one of these evidence records:

- `Supported path evidence`
  - tested TS60 export menu path / operator steps
  - observed app behavior
  - resulting imported file artifact or equivalent proof of whole-file success
- `Guidance-only fallback evidence`
  - tested TS60 export menu path that failed
  - observed failure point
  - explicit app guidance/specification stating the unsupported mode and the supported path or limitation for this release

This evidence may live in implementation notes, release notes, or verification records, but it must exist so the ship decision is objective.

## Implementation Notes For Planning

The implementation plan may start as a compatibility-focused refactor, but it must include an architectural escape hatch:

- if root-cause investigation shows that existing state ownership is too entangled for safe incremental repair, the plan must explicitly switch from "refactor in place" to "targeted structural rewrite" for the collector screen and state orchestration
- after root-cause investigation, the implementation plan must include an explicit decision gate that records whether the work stays in-place refactor or escalates to targeted rewrite before broader code changes continue

That escalation is considered success, not scope failure, if it is required to produce a stable release.
