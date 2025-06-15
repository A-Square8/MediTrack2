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
import android.app.NotificationManager

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_TAKEN = "com.example.meditrack.ACTION_MARK_TAKEN"
        private var currentMediaPlayer: MediaPlayer? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        // Add null safety checks
        if (context == null || intent == null) return

        val action = intent.action

        when (action) {
            ACTION_MARK_TAKEN -> {
                handleMarkAsTaken(context, intent)
                return
            }
            else -> {
                // Handle regular alarm notification
                handleAlarmNotification(context, intent)
            }
        }
    }

    private fun handleMarkAsTaken(context: Context, intent: Intent) {
        val medicineId = intent.getLongExtra("medicine_id", -1)
        val notificationId = intent.getIntExtra("notification_id", -1)

        if (medicineId != -1L) {
            // Update database - mark medicine as taken
            val dbHelper = DatabaseHelper(context)
            dbHelper.updateMedicineStatus(medicineId, true)

            // Stop the alarm sound
            stopAlarmSound()

            // Cancel the notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            // Show confirmation notification
            val notificationHelper = NotificationHelper(context)
            notificationHelper.showSimpleNotification(
                "Medicine Taken",
                "Marked as taken successfully",
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            )
        }
    }

    private fun handleAlarmNotification(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra("medicine_name") ?: "Medicine"
        val dose = intent.getStringExtra("dose") ?: ""
        val medicineId = intent.getLongExtra("medicine_id", -1)
        val type = intent.getStringExtra("type") ?: "reminder"

        // Only play sound for reminder notifications, not follow-up
        if (type == "reminder") {
            playAlarmSound(context)
        }

        // Create notification with "Mark As Taken" action
        val notificationHelper = NotificationHelper(context)
        val notificationId = medicineId.toInt()

        notificationHelper.showMedicineNotification(
            "Medicine Reminder",
            "Time to take $medicineName - $dose",
            notificationId,
            medicineId
        )
    }

    private fun playAlarmSound(context: Context) {
        try {
            // Stop any existing alarm sound
            stopAlarmSound()

            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            currentMediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                } else {
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                }

                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }

            // Stop the alarm after 30 seconds if not manually stopped
            Handler(Looper.getMainLooper()).postDelayed({
                stopAlarmSound()
            }, 30_000)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarmSound() {
        currentMediaPlayer?.let { mediaPlayer ->
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
            currentMediaPlayer = null
        }
    }
}
