package com.focusremind.app.location

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Debounce mechanism shared by WiFi (home/work) and Bluetooth (car)
 * detection: a "connected" event schedules a delayed check instead of
 * firing reminders immediately; a "disconnected" event (or connecting to a
 * DIFFERENT network/device) cancels that same pending check before it runs.
 * Only if the connection holds for the whole delay does anything actually
 * fire — filters out brief, passing connections (e.g. walking past the
 * house WiFi's range without actually going in).
 */
object LocationTriggerScheduler {

    private fun uniqueWorkName(trigger: String) = "location_check_$trigger"

    fun scheduleCheck(context: Context, trigger: String, delaySeconds: Int) {
        val request = OneTimeWorkRequestBuilder<LocationCheckWorker>()
            .setInitialDelay(delaySeconds.toLong(), TimeUnit.SECONDS)
            .setInputData(Data.Builder().putString(LocationCheckWorker.KEY_TRIGGER, trigger).build())
            .build()
        // REPLACE: a new "connected" event for the same trigger restarts the
        // debounce window from zero rather than stacking duplicate checks.
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(trigger), ExistingWorkPolicy.REPLACE, request
        )
    }

    fun cancelCheck(context: Context, trigger: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(trigger))
    }
}
