package com.example.meditrack

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val medicineName = intent?.getStringExtra("medicine_name") ?: "Medicine"
            val dose = intent?.getStringExtra("dose") ?: ""

            try {
                val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val mediaPlayer = MediaPlayer()
                mediaPlayer.setDataSource(context, alarmUri)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mediaPlayer.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                } else {
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM)
                }

                mediaPlayer.isLooping = true
                mediaPlayer.setVolume(1.0f, 1.0f)
                mediaPlayer.prepare()
                mediaPlayer.start()

                // Stop the alarm after 20 seconds
                Handler(Looper.getMainLooper()).postDelayed({
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.stop()
                        mediaPlayer.release()
                    }
                }, 20_000)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Show the notification as before
            val notificationHelper = NotificationHelper(context)
            notificationHelper.showNotification(
                "Medicine Reminder",
                "Time to take $medicineName - $dose",
                System.currentTimeMillis().toInt()
            )
        }
    }
}
