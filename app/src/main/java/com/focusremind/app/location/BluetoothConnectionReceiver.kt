package com.focusremind.app.location

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Listens for Bluetooth connect/disconnect — matches against the "car"
 * device configured in Settings. Connecting starts the debounce timer
 * (default 30s, configurable); disconnecting cancels it immediately.
 */
class BluetoothConnectionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BluetoothConnReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val device: BluetoothDevice? = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        val address = try { device?.address } catch (_: SecurityException) { null } ?: return

        val prefs = context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
        val carAddress = prefs.getString("car_bt_address", null) ?: return
        if (!address.equals(carAddress, ignoreCase = true)) return

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val delay = prefs.getInt("bt_debounce_seconds", 30)
                Log.d(TAG, "Car Bluetooth connected — scheduling check in ${delay}s")
                LocationTriggerScheduler.scheduleCheck(context, "CAR", delay)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                Log.d(TAG, "Car Bluetooth disconnected — cancelling pending check")
                LocationTriggerScheduler.cancelCheck(context, "CAR")
            }
        }
    }
}
