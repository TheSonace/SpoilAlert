package com.example.spoilalert.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.spoilalert.MainActivity
import com.example.spoilalert.R
import kotlin.properties.Delegates

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val data = intent!!.getStringExtra("description")!!
        val channelId = intent.getStringExtra("channel")!!
        if (context == null) return
        startNotification(context, data, channelId)
    }

    private fun startNotification(context: Context, description: String, channelid: String) {
        var requestCode by Delegates.notNull<Int>()
        if (channelid == "Orange Lead-Time") {
            requestCode = 100
        }
        if (channelid == "Red Lead-Time") {
            requestCode = 101
        }

        val channel = NotificationChannel(
            channelid, channelid,
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "description"
        context.getSystemService(NotificationManager::class.java).apply {
            createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_MUTABLE)

        val notification = NotificationCompat.Builder(context, channelid)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(context.resources.getColor(R.color.green))
            .setContentTitle("An item may be reaching its spoildate!")
            .setContentText(description)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = 100
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
        Log.e("notification", notification.toString())
    }
}