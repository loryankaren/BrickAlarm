package com.example.brickalarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {

    private val CHANNEL_ID = "alarm_channel"
    private val NOTIFICATION_ID = 123

    override fun onReceive(context: Context, intent: Intent) {
        // Создаем канал уведомлений (если он еще не создан)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Канал будильника"
            val descriptionText = "Канал для уведомлений будильника"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Создаем Intent для запуска MainActivity
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Создаем PendingIntent
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Создаем и отображаем уведомление
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon) // Замените на вашу иконку
            .setContentTitle("Будильник")
            .setContentText("Будильник сработал!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // Прикрепляем PendingIntent
            .setAutoCancel(true) // Добавляем флаг для автоматического удаления

        // Проверяем разрешение на показ уведомлений
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        } else {
            // Разрешение не предоставлено, обрабатываем ситуацию
            // ...
        }

        // ... остальной код ...
    }
}