package com.focusremind.app.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.focusremind.app.FocusRemindApp
import com.focusremind.app.MainActivity
import com.focusremind.app.R

/**
 * Single source of truth for building the alarm notification.
 *
 * Both AlarmReceiver (primary, exact-time path) and ReminderWorker
 * (WorkManager backup path) call this — so the notification always
 * looks the same (amber background, always-visible action buttons)
 * no matter which path actually fires.
 */
object ReminderNotificationBuilder {

    fun build(context: Context, reminderId: Long, title: String): Notification {
        val notificationText = buildNotificationText(title)

        val tapIntent = PendingIntent.getActivity(
            context, reminderId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("reminder_id", reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = PendingIntent.getBroadcast(
            context, (reminderId * 10).toInt(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "DONE"
                putExtra("reminder_id", reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze5Intent = PendingIntent.getBroadcast(
            context, (reminderId * 10 + 1).toInt(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "SNOOZE_5"
                putExtra("reminder_id", reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze15Intent = PendingIntent.getBroadcast(
            context, (reminderId * 10 + 2).toInt(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "SNOOZE_15"
                putExtra("reminder_id", reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze30Intent = PendingIntent.getBroadcast(
            context, (reminderId * 10 + 3).toInt(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = "SNOOZE_30"
                putExtra("reminder_id", reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Custom collapsed view — title only, no buttons (native action buttons
        // below handle Done/+5/+15 — see .addAction() calls). Keeping this view
        // short means it fits within Android's collapsed-notification height
        // limit, so the title is visible without expanding.
        val collapsedView = RemoteViews(context.packageName, R.layout.notification_collapsed).apply {
            setTextViewText(R.id.notif_title, notificationText)
        }

        // Custom expanded view — title only (no extra instruction text)
        val expandedView = RemoteViews(context.packageName, R.layout.notification_expanded).apply {
            setTextViewText(R.id.notif_title, notificationText)
        }

        return NotificationCompat.Builder(context, FocusRemindApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(notificationText)
            .setContentText("Dotknij aby otworzyć")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(tapIntent, true)
            .setContentIntent(tapIntent)
            // Swiping the notification away (dismiss) still snoozes 5 min
            // internally — a quick, low-friction default for an accidental
            // swipe — even though "+5 min" is no longer its own visible
            // button (replaced by +15 / +30 per user preference).
            .setDeleteIntent(snooze5Intent)
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .addAction(android.R.drawable.checkbox_on_background, "✅ Zrobione", doneIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "+15 min", snooze15Intent)
            .addAction(android.R.drawable.ic_menu_recent_history, "+30 min", snooze30Intent)
            .build()
    }

    /**
     * Turns the raw spoken/typed command into the "Przypominam ..." phrasing
     * shown everywhere a reminder's title is displayed (notification, home
     * list, recurring list, history) — not just in the notification.
     *
     * "Przypomnij mi o czymś"      -> "Przypominam o czymś"
     * "Przypomnij mi żeby X"       -> "Przypominam, żeby X"
     * "Przypomnij mi żebym X"      -> "Przypominam, żebym X" (and żebyś/żebyśmy/żebyście)
     * "Zadzwoń do mamy" (no lead-in prefix spoken) -> "Przypominam: Zadzwoń do mamy"
     */
    fun buildNotificationText(title: String): String {
        var cleanTitle = title.trim()
        cleanTitle = cleanTitle.replace(Regex("^przypomnij\\s*(mi\\s*)?", RegexOption.IGNORE_CASE), "")
        cleanTitle = cleanTitle.replace(Regex("^przypomnienie\\s*", RegexOption.IGNORE_CASE), "")
        cleanTitle = cleanTitle.trim()
        // Matches all personal conjugations: żeby, żebym, żebyś, żebyśmy, żebyście
        // (and the unaccented "zeby..." variants some speech recognizers produce)
        val zebyForm = Regex("^[żz]eby(m|ś|śmy|ście)?\\s", RegexOption.IGNORE_CASE)
        return when {
            cleanTitle.startsWith("o ", ignoreCase = true) -> "Przypominam $cleanTitle"
            zebyForm.containsMatchIn(cleanTitle) -> "Przypominam, $cleanTitle"
            else -> "Przypominam: $cleanTitle"
        }
    }
}
