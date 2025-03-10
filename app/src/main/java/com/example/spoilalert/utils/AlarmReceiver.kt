package com.example.spoilalert.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.spoilalert.MainActivity
import com.example.spoilalert.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.e("notification", context.toString())
        if (context == null) return
        startNotification(context)
    }

    private fun startNotification(context: Context) {
        val channelId = "100"
        val channel = NotificationChannel(
            channelId, "my channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "description"
        context.getSystemService(NotificationManager::class.java).apply {
            createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 100, intent, PendingIntent.FLAG_MUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Title")
            .setContentText("Text")
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = 100
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
        Log.e("notification", notification.toString())
    }
}