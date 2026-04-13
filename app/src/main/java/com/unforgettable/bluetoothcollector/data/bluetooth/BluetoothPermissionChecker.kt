package com.unforgettable.bluetoothcollector.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class BluetoothPermissionChecker(
    private val context: Context,
) {
    fun currentState(): BluetoothPermissionState {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val (bluetoothEnabled, bluetoothStateTrusted) = when {
            adapter == null -> false to true
            else -> runCatching { adapter.isEnabled to true }.getOrElse { false to false }
        }
        return BluetoothPermissionState(
            canDiscover = hasDiscoveryPermission(),
            canConnect = hasConnectPermission(),
            canAdvertise = hasAdvertisePermission(),
            bluetoothEnabled = bluetoothEnabled,
            bluetoothStateTrusted = bluetoothStateTrusted,
        )
    }

    fun requiredPermissionsForDiscovery(): List<String> {
        return buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    fun requiredPermissionsForConnect(): List<String> {
        return buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
    }

    fun requiredPermissionsForReceiver(): List<String> {
        return buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
        }.distinct()
    }

    private fun hasDiscoveryPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isGranted(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            isGranted(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isGranted(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            true
        }
    }

    private fun hasAdvertisePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isGranted(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            true
        }
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val LEGACY_DISCOVERY_BOND_STATE_UNKNOWN: Int = -1
    }
}
