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

        // Custom collapsed view — buttons always visible, amber background
        val collapsedView = RemoteViews(context.packageName, R.layout.notification_collapsed).apply {
            setTextViewText(R.id.notif_title, notificationText)
            setOnClickPendingIntent(R.id.btn_done, doneIntent)
            setOnClickPendingIntent(R.id.btn_snooze5, snooze5Intent)
            setOnClickPendingIntent(R.id.btn_snooze15, snooze15Intent)
        }

        // Custom expanded view — same buttons + extra space
        val expandedView = RemoteViews(context.packageName, R.layout.notification_expanded).apply {
            setTextViewText(R.id.notif_title, notificationText)
            setTextViewText(R.id.notif_text, "Dotknij aby otworzyć • Przesuń w bok aby odrzucić")
            setOnClickPendingIntent(R.id.btn_done, doneIntent)
            setOnClickPendingIntent(R.id.btn_snooze5, snooze5Intent)
            setOnClickPendingIntent(R.id.btn_snooze15, snooze15Intent)
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
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .addAction(android.R.drawable.checkbox_on_background, "✅ Gotowe", doneIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "+5 min", snooze5Intent)
            .addAction(android.R.drawable.ic_menu_recent_history, "+15 min", snooze15Intent)
            .build()
    }

    private fun buildNotificationText(title: String): String {
        var cleanTitle = title
        cleanTitle = cleanTitle.replace(Regex("^przypomnij\\s*(mi\\s*)?", RegexOption.IGNORE_CASE), "")
        cleanTitle = cleanTitle.replace(Regex("^przypomnienie\\s*", RegexOption.IGNORE_CASE), "")
        cleanTitle = cleanTitle.trim()
        return if (cleanTitle.startsWith("o ") || cleanTitle.startsWith("O ")) {
            "Przypominam $cleanTitle"
        } else {
            "Przypominam: $cleanTitle"
        }
    }
}
