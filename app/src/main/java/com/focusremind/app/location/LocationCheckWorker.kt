package com.focusremind.app.location

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Runs after the debounce delay (30s for Bluetooth/car, 3 min for WiFi/home
 * /work by default) — if it actually gets to run at all, that means the
 * connection held for the whole debounce window (a disconnect during that
 * window cancels this same unique work before it ever executes, via
 * WorkManager's enqueueUniqueWork REPLACE policy).
 */
class LocationCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TRIGGER = "trigger"
    }

    override suspend fun doWork(): Result {
        val trigger = inputData.getString(KEY_TRIGGER) ?: return Result.failure()
        LocationFirer.fireIfNewArrival(applicationContext, trigger)
        return Result.success()
    }
}
