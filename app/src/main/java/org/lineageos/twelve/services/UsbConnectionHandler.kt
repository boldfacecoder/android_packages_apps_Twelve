/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.services

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.core.content.ContextCompat
import org.lineageos.twelve.ext.exclusiveUsbAccess

class UsbConnectionHandler(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val permissionIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
        PendingIntent.FLAG_IMMUTABLE
    )

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            Log.d(LOG_TAG, "Permission granted for device $device")
                            // Permission granted, standard AudioTrack routing should pick this up
                            // if it's the default or routed correctly.
                            // True exclusive access might require more complex handling not standard in Android APIs.
                        }
                    } else {
                        Log.d(LOG_TAG, "permission denied for device $device")
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                 if (sharedPreferences.exclusiveUsbAccess) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    device?.let { requestPermission(it) }
                 }
            }
        }
    }

    fun start() {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        ContextCompat.registerReceiver(context, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        // Check for already connected devices
        if (sharedPreferences.exclusiveUsbAccess) {
            usbManager.deviceList.values.forEach { device ->
                requestPermission(device)
            }
        }
    }

    fun stop() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }

    private fun requestPermission(device: UsbDevice) {
        // Simple heuristic to check if it's an audio device (Class 1) or has audio interface
        if (isAudioDevice(device)) {
             if (!usbManager.hasPermission(device)) {
                usbManager.requestPermission(device, permissionIntent)
            }
        }
    }

    private fun isAudioDevice(device: UsbDevice): Boolean {
        // Check device class or interface classes
        if (device.deviceClass == 1) return true // USB_CLASS_AUDIO

        for (i in 0 until device.interfaceCount) {
            if (device.getInterface(i).interfaceClass == 1) return true
        }
        return false
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "org.lineageos.twelve.USB_PERMISSION"
        private const val LOG_TAG = "UsbConnectionHandler"
    }
}
