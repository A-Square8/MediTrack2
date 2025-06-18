package com.example.meditrack

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
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
import java.text.SimpleDateFormat
import java.util.*

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
        val dayOfWeek = intent.getIntExtra("day_of_week", -1)

        if (medicineId != -1L) {
            // Update main database
            val dbHelper = DatabaseHelper(context)
            dbHelper.updateMedicineStatus(medicineId, true)

            // Update analytics database (keep Version 2 feature)
            val analyticsDb = AnalyticsDbHelper(context)
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            analyticsDb.logMedicineTaken(medicineId, currentTime, dateString)

            // Cancel follow-up alarm for today
            if (dayOfWeek != -1) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val followUpIntent = Intent(context, AlarmReceiver::class.java)
                val followUpPendingIntent = PendingIntent.getBroadcast(
                    context,
                    (medicineId.toInt() * 10 + dayOfWeek + 100),
                    followUpIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(followUpPendingIntent)
            }

            // Stop alarm sound and cancel notification
            stopAlarmSound()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            // Show confirmation
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
        val scheduledDayOfWeek = intent.getIntExtra("day_of_week", -1)

        // Validate if today matches the scheduled day
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (scheduledDayOfWeek != -1 && scheduledDayOfWeek != today) {
            return
        }

        // Check if medicine is already taken today
        val dbHelper = DatabaseHelper(context)
        val medicine = dbHelper.getAllMedicines().find { it.id == medicineId }
        if (medicine?.isConsumed == true && type == "followup") {
            return // Don't show follow-up if already taken
        }

        // Only play sound for reminder notifications
        if (type == "reminder") {
            playAlarmSound(context)
        }

        // Create notification
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
