package com.focusremind.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.focusremind.app.MainActivity
import com.focusremind.app.R

class ReminderWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            // Mic circle: opens the app AND starts recording immediately
            val micIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_voice", true)
                putExtra("from_widget", true)
            }
            val micPending = PendingIntent.getActivity(
                context, id * 10, micIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Rest of the widget (background, text): just opens the app,
            // no auto-recording, but still skips the splash — any tap on
            // the widget should feel instant. Different request code
            // (id * 10 + 1) is required — reusing the same code as
            // micPending would make Android silently treat both as the
            // same PendingIntent and overwrite its extras, breaking the
            // split entirely.
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("from_widget", true)
            }
            val openPending = PendingIntent.getActivity(
                context, id * 10 + 1, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
                setOnClickPendingIntent(R.id.widget_root, openPending)
                setOnClickPendingIntent(R.id.widget_mic, micPending)
            }
            manager.updateAppWidget(id, views)
        }
    }
}
