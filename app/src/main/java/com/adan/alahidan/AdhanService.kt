package com.adan.alahidan

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AdhanService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var previousVolume: Int = -1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_ADHAN") {
            stopSelf()
            return START_NOT_STICKY
        }

        val prayerName = intent?.getStringExtra("prayer_name") ?: "الصلاة"
        createChannel()

        val stopIntent = Intent(this, AdhanService::class.java).apply { action = "STOP_ADHAN" }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "adhan_channel")
            .setContentTitle("حان الآن موعد أذان $prayerName")
            .setContentText("بصوت الشيخ محمد اللحيدان")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPending)
            .addAction(0, "إيقاف الأذان", stopPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(101, notification)

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (previousVolume == -1) {
            previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        }
        val targetVol = (maxVol * 0.90).toInt().coerceAtLeast(1)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, R.raw.adhan_luhaidan)?.apply {
            setOnCompletionListener { stopSelf() }
            start()
        }

        return START_NOT_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "adhan_channel",
                "تنبيهات الأذان",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        if (previousVolume != -1) {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0)
            previousVolume = -1
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
