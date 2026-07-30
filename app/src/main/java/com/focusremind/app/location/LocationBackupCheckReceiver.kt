package com.focusremind.app.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Periodic safety net: checks the CURRENTLY connected WiFi network directly,
 * independent of whether the live NetworkCallback (WifiConnectionObserver)
 * happened to be registered when the actual connection event occurred.
 * Android instantiates a fresh app process to deliver this AlarmManager
 * broadcast even if Nomi wasn't running at all — unlike the live observer,
 * which only works while the process is already alive.
 */
class LocationBackupCheckReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "LocationBackupCheck"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val raw = wifiManager.connectionInfo?.ssid
                val ssid = if (raw.isNullOrBlank() || raw == WifiManager.UNKNOWN_SSID) null else raw.trim('"')

                val prefs = context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
                val homeSsid = prefs.getString("home_wifi_ssid", null)
                val workSsid = prefs.getString("work_wifi_ssid", null)

                when {
                    ssid != null && homeSsid != null && ssid == homeSsid -> {
                        Log.d(TAG, "Backup check: currently on home WiFi")
                        LocationFirer.fireIfNewArrival(context, "HOME")
                        LocationFirer.markAbsent(context, "WORK")
                    }
                    ssid != null && workSsid != null && ssid == workSsid -> {
                        Log.d(TAG, "Backup check: currently on work WiFi")
                        LocationFirer.fireIfNewArrival(context, "WORK")
                        LocationFirer.markAbsent(context, "HOME")
                    }
                    else -> {
                        LocationFirer.markAbsent(context, "HOME")
                        LocationFirer.markAbsent(context, "WORK")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Backup check failed", e)
            } finally {
                // Always reschedule the next check, success or failure.
                LocationBackupScheduler.scheduleNext(context)
                pendingResult.finish()
            }
        }
    }
}
