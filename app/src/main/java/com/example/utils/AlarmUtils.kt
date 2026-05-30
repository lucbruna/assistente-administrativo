package com.example.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.data.AgendaItem

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getIntExtra("item_id", 0)
        val title = intent.getStringExtra("title") ?: "Compromisso"
        AlarmUtils.showAlarmNotification(context, title, itemId)
    }
}

object AlarmUtils {

    private const val CHANNEL_ID = "agenda_alarms"
    private const val ACTION_ALARM_TRIGGERED = "com.example.ALARM_TRIGGERED"

    fun scheduleAlarm(context: Context, agendaItem: AgendaItem) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGERED
            putExtra("item_id", agendaItem.id)
            putExtra("title", agendaItem.title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            agendaItem.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, agendaItem.dateTime, pendingIntent)
    }

    fun cancelAlarm(context: Context, agendaItemId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGERED
        }
        PendingIntent.getBroadcast(
            context,
            agendaItemId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.cancel()
    }

    fun showAlarmNotification(context: Context, title: String, itemId: Int) {
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            "Lembretes de Agenda",
            android.app.NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            itemId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("item_id", itemId)
            putExtra("title", title)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            itemId + 1000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeTime = System.currentTimeMillis() + 15 * 60 * 1000
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            snoozeTime,
            snoozePendingIntent
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("\uD83D\uDCC5 Compromisso: $title")
            .setContentText("Você tem um compromisso agora!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                "Adiar 15 min",
                snoozePendingIntent
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(itemId, notification)
            }
        } else {
            NotificationManagerCompat.from(context).notify(itemId, notification)
        }
    }
}
