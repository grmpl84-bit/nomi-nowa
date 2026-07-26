package com.focusremind.app.location

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.util.Log

/**
 * Observes WiFi connections via ConnectivityManager.NetworkCallback (the
 * modern, non-deprecated way — unlike WIFI state broadcasts, this isn't
 * restricted by the implicit-broadcast limits introduced in Android 8, so
 * it's registered once, persistently, in FocusRemindApp rather than
 * declared in the manifest).
 */
object WifiConnectionObserver {

    private const val TAG = "WifiConnectionObserver"
    private var registered = false

    fun register(context: Context) {
        if (registered) return
        registered = true

        val appContext = context.applicationContext
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val ssid = extractSsid(capabilities) ?: return
                checkSsid(appContext, ssid)
            }

            override fun onLost(network: Network) {
                // Left WiFi range entirely (any network) — cancel any pending
                // checks for both home and work, whichever might be running.
                Log.d(TAG, "WiFi lost — cancelling pending home/work checks")
                LocationTriggerScheduler.cancelCheck(appContext, "HOME")
                LocationTriggerScheduler.cancelCheck(appContext, "WORK")
            }
        })
    }

    private fun extractSsid(capabilities: NetworkCapabilities): String? {
        val info = capabilities.transportInfo as? WifiInfo ?: return null
        // WifiInfo.getSSID() wraps the name in quotes normally
        val raw = info.ssid ?: return null
        if (raw == WifiManager.UNKNOWN_SSID) return null // location permission not granted
        return raw.trim('"')
    }

    private fun checkSsid(context: Context, ssid: String) {
        val prefs = context.getSharedPreferences("focusremind_settings", Context.MODE_PRIVATE)
        val homeSsid = prefs.getString("home_wifi_ssid", null)
        val workSsid = prefs.getString("work_wifi_ssid", null)
        val delay = prefs.getInt("wifi_debounce_seconds", 180)

        when {
            homeSsid != null && ssid == homeSsid -> {
                Log.d(TAG, "Connected to home WiFi ($ssid) — scheduling check in ${delay}s")
                LocationTriggerScheduler.scheduleCheck(context, "HOME", delay)
                LocationTriggerScheduler.cancelCheck(context, "WORK")
            }
            workSsid != null && ssid == workSsid -> {
                Log.d(TAG, "Connected to work WiFi ($ssid) — scheduling check in ${delay}s")
                LocationTriggerScheduler.scheduleCheck(context, "WORK", delay)
                LocationTriggerScheduler.cancelCheck(context, "HOME")
            }
            else -> {
                // Connected to some OTHER network — neither home nor work.
                LocationTriggerScheduler.cancelCheck(context, "HOME")
                LocationTriggerScheduler.cancelCheck(context, "WORK")
            }
        }
    }
}
