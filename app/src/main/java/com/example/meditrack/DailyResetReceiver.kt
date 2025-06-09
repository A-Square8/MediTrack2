package com.example.meditrack

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val dbHelper = DatabaseHelper(context)
            dbHelper.resetDailyCheckboxes()
        }
    }
}