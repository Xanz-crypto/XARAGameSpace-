package com.xara.gamespace.services

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.xara.gamespace.XaraApplication

class SessionForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val gameName = intent?.getStringExtra(EXTRA_GAME_NAME) ?: "Gaming Session"
        val openIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, XaraApplication.CHANNEL_SESSION_HUD)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("XARAGameSpace Session Active")
            .setContentText("Monitoring $gameName telemetry & thermal headroom")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    companion object {
        const val EXTRA_GAME_NAME = "extra_game_name"
        private const val NOTIFICATION_ID = 2001
    }
}
