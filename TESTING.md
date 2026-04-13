# Hardware Testing Guide - v1.5.0

## Overview

This document provides a comprehensive testing checklist for v1.5.0, which includes TS09/TS60 export-path split work, TS09 import completion fixes, and TS60 experimental receiver-mode hardening.

## Test Environment

### Required Hardware
- Android device (Android 8.0+ / API 26+)
- Leica TS60 total station (or TS16/TS50/MS60)
- Bluetooth enabled on both devices
- Charged batteries

### Optional Hardware
- Leica FlexLine instrument (TS02-TS13) for backward compatibility testing

## Pre-Test Setup

### 1. Install APK
```bash
# Download from GitHub release
wget https://github.com/unf0rgettable-h/blue_tooth/releases/download/v1.5.0/survlink-v1.5.0-signed.apk

# Install via adb
adb install survlink-v1.5.0-signed.apk

# Or manually install on device
```

### 2. Verify Installation
- [ ] App icon appears in launcher
- [ ] App opens without crash
- [ ] Version shows "1.5.0" in app info

### 3. Grant Permissions
- [ ] Bluetooth permission granted
- [ ] Location permission granted (required for Bluetooth scanning on Android 10+)
- [ ] Nearby devices permission granted (Android 12+)

## Test Cases

### TC-001: Basic Connectivity (FlexLine - Backward Compatibility)

**Objective**: Verify FlexLine instruments still work with passive GSI-Online protocol

**Preconditions**: 
- FlexLine instrument (TS02-TS13) available
- Instrument paired with Android device

**Steps**:
1. Open SurvLink app
2. Go to "蓝牙" page
3. Select brand "Leica", model "TS09" (or other FlexLine model)
4. Select paired device from list
5. Tap "连接"
6. Go to "数据" page
7. Tap "开始接收"
8. Take measurement on instrument

**Expected Results**:
- [ ] Connection established successfully
- [ ] "开始接收" button appears (not GeoCOM controls)
- [ ] Measurement data appears automatically in preview
- [ ] Data is selectable/copyable
- [ ] Export works correctly

**Status**: ⏳ Pending

---

### TC-002: TS60 Connection

**Objective**: Verify TS60 connects and is recognized as Captivate instrument

**Preconditions**: 
- TS60 instrument available
- TS60 paired with Android device

**Steps**:
1. Open SurvLink app
2. Go to "蓝牙" page
3. Select brand "Leica", model "TS60"
4. Select paired TS60 from device list
5. Tap "连接"

**Expected Results**:
- [ ] Connection established successfully
- [ ] No error messages
- [ ] Connection indicator shows connected state
- [ ] App recognizes TS60 as Captivate instrument

**Status**: ⏳ Pending

---

### TC-003: GeoCOM Control Panel Display

**Objective**: Verify GeoCOM control panel appears for TS60

**Preconditions**: 
- TS60 connected (TC-002 passed)

**Steps**:
1. Go to "数据" page
2. Observe UI controls

**Expected Results**:
- [ ] "Start Auto" button visible
- [ ] "Measure Once" button visible
- [ ] Both buttons enabled when connected
- [ ] No "开始接收" button (passive mode button)
- [ ] GeoCOM control panel clearly distinguishable from passive mode

**Status**: ⏳ Pending

---

### TC-004: Single-Shot Measurement

**Objective**: Verify single-shot measurement mode works

**Preconditions**: 
- TS60 connected (TC-002 passed)
- GeoCOM control panel visible (TC-003 passed)

**Steps**:
1. Point TS60 at a target
2. Tap "Measure Once" button
3. Wait for measurement to complete
4. Observe preview list

**Expected Results**:
- [ ] Button shows loading indicator during measurement
- [ ] Measurement completes within 5 seconds
- [ ] New record appears in preview list
- [ ] Record shows:
  - Horizontal angle (Hz) in degrees
  - Vertical angle (V) in degrees
  - Slope distance in meters
- [ ] Values are formatted to 4 decimal places (angles) and 3 decimal places (distance)
- [ ] Button returns to enabled state after measurement

**Status**: ⏳ Pending

---

### TC-005: Continuous Polling Mode

**Objective**: Verify continuous polling mode works

**Preconditions**: 
- TS60 connected (TC-002 passed)
- GeoCOM control panel visible (TC-003 passed)

**Steps**:
1. Point TS60 at a target
2. Tap "Start Auto" button
3. Observe measurements appearing
4. Wait for at least 10 seconds (5 measurements)
5. Tap "Stop" button

**Expected Results**:
- [ ] Button changes to "Stop" with progress indicator
- [ ] Measurements appear approximately every 2 seconds
- [ ] Each measurement shows Hz/V angles and distance
- [ ] Preview list updates in real-time
- [ ] "Measure Once" button disabled during polling
- [ ] "Stop" button stops polling
- [ ] Button returns to "Start Auto" after stopping

**Status**: ⏳ Pending

---

### TC-006: Angle Display Accuracy

**Objective**: Verify angle values are displayed correctly

**Preconditions**: 
- TS60 connected (TC-002 passed)
- At least one measurement taken (TC-004 or TC-005 passed)

**Steps**:
1. Take measurement with known angle (e.g., 0°, 90°, 180°)
2. Compare displayed value with TS60 screen
3. Verify angle format

**Expected Results**:
- [ ] Horizontal angle matches TS60 display (±0.0001°)
- [ ] Vertical angle matches TS60 display (±0.0001°)
- [ ] Distance matches TS60 display (±0.001m)
- [ ] Angles displayed in degrees (not radians or gon)
- [ ] Format: "Hz: 123.4567°", "V: 89.1234°", "Dist: 45.678m"

**Status**: ⏳ Pending

---

### TC-007: CSV Export - GeoCOM Format

**Objective**: Verify CSV export includes multi-unit angles

**Preconditions**: 
- TS60 connected (TC-002 passed)
- At least 3 measurements taken (TC-004 or TC-005 passed)

**Steps**:
1. Tap "导出并分享"
2. Select CSV format
3. Share to file viewer or save to device
4. Open CSV file
5. Inspect columns and values

**Expected Results**:
- [ ] CSV file created successfully
- [ ] Header row contains: Timestamp, Model, HzAngle(rad), HzAngle(deg), HzAngle(gon), VAngle(rad), VAngle(deg), VAngle(gon), SlopeDist(m)
- [ ] Each measurement row contains all 9 columns
- [ ] Angle conversions are correct:
  - radians = degrees × π / 180
  - gon = radians × 200 / π
- [ ] Values match displayed measurements
- [ ] File is readable in Excel/Google Sheets

**Status**: ⏳ Pending

---

### TC-008: Angle Unit Conversions

**Objective**: Verify angle conversions are mathematically correct

**Preconditions**: 
- CSV export completed (TC-007 passed)

**Steps**:
1. Open exported CSV
2. Pick one measurement row
3. Verify conversions manually:
   - deg = rad × 180 / π
   - gon = rad × 200 / π
   - gon = deg × 200 / 180

**Expected Results**:
- [ ] Radians to degrees conversion correct (±0.0001°)
- [ ] Radians to gon conversion correct (±0.0001 gon)
- [ ] All three units represent the same angle
- [ ] Example: 90° = 1.5708 rad = 100 gon

**Status**: ⏳ Pending

---

### TC-009: Connection Stability

**Objective**: Verify connection remains stable during extended use

**Preconditions**: 
- TS60 connected (TC-002 passed)

**Steps**:
1. Start continuous polling mode
2. Let it run for 5 minutes (150 measurements)
3. Observe for disconnections or errors

**Expected Results**:
- [ ] No disconnections during 5-minute test
- [ ] All measurements captured successfully
- [ ] No timeout errors
- [ ] No deadlock or freeze
- [ ] Memory usage remains stable
- [ ] Battery drain is reasonable

**Status**: ⏳ Pending

---

### TC-010: Disconnect and Reconnect

**Objective**: Verify app handles disconnect/reconnect gracefully

**Preconditions**: 
- TS60 connected (TC-002 passed)

**Steps**:
1. Start continuous polling mode
2. Turn off TS60 Bluetooth or move out of range
3. Wait for disconnect detection
4. Turn on TS60 Bluetooth or move back in range
5. Reconnect from app

**Expected Results**:
- [ ] App detects disconnect within 5 seconds
- [ ] Polling stops automatically
- [ ] UI shows disconnected state
- [ ] Reconnect works without app restart
- [ ] Previous measurements preserved
- [ ] New session starts correctly

**Status**: ⏳ Pending

---

### TC-011: Error Handling - Invalid Response

**Objective**: Verify app handles invalid GeoCOM responses

**Preconditions**: 
- TS60 connected (TC-002 passed)

**Steps**:
1. Trigger measurement when TS60 is not ready (e.g., during initialization)
2. Observe app behavior

**Expected Results**:
- [ ] App does not crash
- [ ] Error is logged (check logcat if possible)
- [ ] User sees appropriate feedback (toast or status message)
- [ ] App recovers and can take next measurement
- [ ] No data corruption

**Status**: ⏳ Pending

---

### TC-012: Room Database Migration

**Objective**: Verify existing data is preserved after update

**Preconditions**: 
- Previous version (v1.3.3) installed with existing measurements
- Measurements saved in database

**Steps**:
1. Note number of existing measurements
2. Install v1.4.0 APK (upgrade, not fresh install)
3. Open app
4. Check measurement history

**Expected Results**:
- [ ] App opens without crash
- [ ] All previous measurements still visible
- [ ] Previous measurements have null GeoCOM fields (expected)
- [ ] New TS60 measurements have populated GeoCOM fields
- [ ] Export of old measurements still works
- [ ] No data loss

**Status**: ⏳ Pending

---

### TC-013: Performance - Battery Drain

**Objective**: Measure battery consumption during continuous polling

**Preconditions**: 
- TS60 connected (TC-002 passed)
- Device fully charged

**Steps**:
1. Note starting battery percentage
2. Start continuous polling mode
3. Run for 30 minutes
4. Note ending battery percentage

**Expected Results**:
- [ ] Battery drain < 10% per 30 minutes
- [ ] No excessive CPU usage
- [ ] Device does not overheat
- [ ] Polling interval remains consistent (2 seconds)

**Status**: ⏳ Pending

---

### TC-014: Concurrent Operations

**Objective**: Verify app handles concurrent operations correctly

**Preconditions**: 
- TS60 connected (TC-002 passed)

**Steps**:
1. Start continuous polling mode
2. While polling, tap "Measure Once" button
3. Observe behavior

**Expected Results**:
- [ ] "Measure Once" button is disabled during polling (expected behavior)
- [ ] OR single measurement is queued and executes after current poll
- [ ] No race conditions or crashes
- [ ] Measurements remain in correct sequence

**Status**: ⏳ Pending

---

### TC-015: UI Responsiveness

**Objective**: Verify UI remains responsive during measurements

**Preconditions**: 
- TS60 connected (TC-002 passed)

**Steps**:
1. Start continuous polling mode
2. Scroll through measurement list
3. Switch between "蓝牙" and "数据" pages
4. Tap various UI elements

**Expected Results**:
- [ ] UI remains responsive (no lag > 100ms)
- [ ] Scrolling is smooth
- [ ] Page transitions are instant
- [ ] No ANR (Application Not Responding) dialogs
- [ ] Background polling continues during UI interactions

**Status**: ⏳ Pending

---

## Test Summary

### Test Execution Checklist

- [ ] All test cases executed
- [ ] All critical tests passed (TC-001 to TC-007)
- [ ] All optional tests passed (TC-008 to TC-015)
- [ ] No blocking issues found
- [ ] Performance acceptable
- [ ] Battery drain acceptable

### Known Issues

_Document any issues found during testing here_

| Issue ID | Severity | Description | Workaround | Status |
|----------|----------|-------------|------------|--------|
| - | - | - | - | - |

### Test Environment Details

- **Device Model**: _____________
- **Android Version**: _____________
- **SurvLink Version**: 1.4.0 (versionCode 8)
- **TS60 Firmware**: _____________
- **Test Date**: _____________
- **Tester**: _____________

### Sign-Off

- [ ] All critical functionality verified
- [ ] No blocking issues
- [ ] Ready for production release

**Tester Signature**: _________________ **Date**: _____________

---

## Troubleshooting

### Issue: Connection fails
- Verify TS60 Bluetooth is enabled
- Check TS60 is not connected to another device
- Try unpairing and re-pairing
- Restart both devices

### Issue: No measurements appear
- Verify TS60 is in measurement mode
- Check TS60 display for errors
- Try single-shot measurement first
- Check logcat for errors: `adb logcat | grep GeoCom`

### Issue: Angles seem incorrect
- Verify TS60 angle units (should be in radians internally)
- Check TS60 firmware version
- Compare with TS60 display values
- Verify CSV export shows correct conversions

### Issue: App crashes
- Collect crash logs: `adb logcat > crash.log`
- Note exact steps to reproduce
- Report on GitHub Issues with logs

---

## Appendix: Expected GeoCOM Response Format

### GetSimpleMeasurement Response
```
%R1P,0,0:0,1.234567,0.567890,123.456\r\n
```

Where:
- `%R1P` = Response prefix
- `0` = RC_COM (communication status, 0 = success)
- `0` = Transaction ID
- `0` = RC (return code, 0 = success)
- `1.234567` = Horizontal angle (radians)
- `0.567890` = Vertical angle (radians)
- `123.456` = Slope distance (meters)

### Angle Conversion Reference

| Degrees | Radians | Gon |
|---------|---------|-----|
| 0° | 0.0000 | 0 |
| 45° | 0.7854 | 50 |
| 90° | 1.5708 | 100 |
| 180° | 3.1416 | 200 |
| 270° | 4.7124 | 300 |
| 360° | 6.2832 | 400 |
