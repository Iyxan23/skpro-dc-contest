package com.iyxan23.slice.domain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

class RemoteControlService : Service() {
    companion object { private const val CHANNEL_ID = "SliceRemoteControlServiceChannel" }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val controller = intent.getStringExtra("controller_name")
        val mediaProjectionToken = intent.getParcelableExtra<Intent>("media_projection_token")

        createNotificationChannel()

//        val notificationIntent = Intent(this, MainActivity::class.java)
//        val pendingIntent = PendingIntent.getActivity(
//            this,
//            0, notificationIntent, 0
//        )

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Slice Remote Control")
            .setContentText("Your device is being controlled by $controller")
//            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        // todo: start using MediaProjection and start doing webrtc stuff

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "SliceRemoteControlServiceChannel",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}