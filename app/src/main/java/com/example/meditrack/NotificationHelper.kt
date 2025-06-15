package com.example.meditrack

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "MEDICINE_REMINDER"
        private const val CHANNEL_NAME = "Medicine Reminders"
        private const val SIMPLE_CHANNEL_ID = "SIMPLE_NOTIFICATIONS"
        private const val SIMPLE_CHANNEL_NAME = "Simple Notifications"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Medicine reminder channel
            val medicineChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for medicine reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            }

            // Simple notification channel
            val simpleChannel = NotificationChannel(
                SIMPLE_CHANNEL_ID,
                SIMPLE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Simple confirmation notifications"
            }

            notificationManager.createNotificationChannel(medicineChannel)
            notificationManager.createNotificationChannel(simpleChannel)
        }
    }

    fun showMedicineNotification(title: String, message: String, notificationId: Int, medicineId: Long) {
        // Create "Mark As Taken" action
        val markTakenIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_MARK_TAKEN
            putExtra("medicine_id", medicineId)
            putExtra("notification_id", notificationId)
        }

        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1000, // Unique request code
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create main notification intent (opens app)
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val mainPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .addAction(
                R.drawable.ic_notification,
                "Mark As Taken",
                markTakenPendingIntent
            )
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    fun showSimpleNotification(title: String, message: String, notificationId: Int) {
        val notification = NotificationCompat.Builder(context, SIMPLE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    // Keep the old method for backward compatibility
    fun showNotification(title: String, message: String, notificationId: Int) {
        showSimpleNotification(title, message, notificationId)
    }
}
