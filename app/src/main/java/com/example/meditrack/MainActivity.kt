package com.example.meditrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.view.View
import android.widget.TextView
import android.app.AlarmManager
import android.app.PendingIntent
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MedicineAdapter
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        val btnAdd: Button = findViewById(R.id.btnAdd)
        val btnView: Button = findViewById(R.id.btnView)
        val btnAnalytics: Button = findViewById(R.id.btnAnalytics)
        recyclerView = findViewById(R.id.recyclerView)

        setupRecyclerView()
        loadMedicines()

        checkAndResetDailyCheckboxes()

        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddMedicineActivity::class.java))
        }

        btnView.setOnClickListener {
            startActivity(Intent(this, ViewAllMedicinesActivity::class.java))
        }

        btnAnalytics.setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadMedicines()
    }

    private fun setupRecyclerView() {
        adapter = MedicineAdapter(
            medicines = emptyList(),
            onCheckboxChanged = { medicine, isChecked ->
                dbHelper.updateMedicineStatus(medicine.id, isChecked)
                if (isChecked) {
                    val analyticsDb = AnalyticsDbHelper(this)
                    val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    val dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    analyticsDb.logMedicineTaken(medicine.id, currentTime, dateString)
                    android.util.Log.d("MainActivity", "Medicine taken logged: ${medicine.name}")

                    cancelFollowUpAlarm(medicine)
                }
            },
            onDeleteClicked = { medicine ->
                deleteMedicine(medicine)
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun deleteMedicine(medicine: Medicine) {
        val analyticsDb = AnalyticsDbHelper(this)
        analyticsDb.logDeletedMedicine(medicine)

        cancelMedicineAlarm(medicine)
        dbHelper.deleteMedicine(medicine.id)
        loadMedicines()
    }

    private fun cancelFollowUpAlarm(medicine: Medicine) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val selectedDays = medicine.days.split(",").map { it.trim() }

        selectedDays.forEach { dayName ->
            val dayOfWeek = when(dayName) {
                "Sunday" -> Calendar.SUNDAY
                "Monday" -> Calendar.MONDAY
                "Tuesday" -> Calendar.TUESDAY
                "Wednesday" -> Calendar.WEDNESDAY
                "Thursday" -> Calendar.THURSDAY
                "Friday" -> Calendar.FRIDAY
                "Saturday" -> Calendar.SATURDAY
                else -> return@forEach
            }

            val intent = Intent(this, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                (medicine.id.toInt() * 10 + dayOfWeek + 100),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }


    private fun loadMedicines() {
        val allMedicines = dbHelper.getAllMedicines()
        val today = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(java.util.Date())
        val todayMedicines = allMedicines.filter { medicine ->
            medicine.days.split(",").map { it.trim() }.contains(today)
        }

        val analyticsDb = AnalyticsDbHelper(this)
        val dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

        // Log each medicine schedule only once per day
        todayMedicines.forEach { medicine ->
            analyticsDb.logMedicineSchedule(medicine, dateString)
            android.util.Log.d("MainActivity", "Medicine scheduled logged: ${medicine.name} for $dateString")
        }

        // Debug: Print analytics data
        analyticsDb.debugPrintData()

        adapter.updateMedicines(todayMedicines)

        val tvEmpty: TextView = findViewById(R.id.tvEmpty)
        if (todayMedicines.isNotEmpty()) {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
        } else {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        }
    }

    private fun checkAndResetDailyCheckboxes() {
        val sharedPrefs = getSharedPreferences("MediTrack", Context.MODE_PRIVATE)
        val lastResetDate = sharedPrefs.getString("last_reset_date", "")
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        if (lastResetDate != currentDate) {
            dbHelper.resetDailyCheckboxes()
            sharedPrefs.edit()
                .putString("last_reset_date", currentDate)
                .apply()
        }
    }

    private fun cancelMedicineAlarm(medicine: Medicine) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val selectedDays = medicine.days.split(",").map { it.trim() }

        selectedDays.forEach { dayName ->
            val dayOfWeek = when(dayName) {
                "Sunday" -> Calendar.SUNDAY
                "Monday" -> Calendar.MONDAY
                "Tuesday" -> Calendar.TUESDAY
                "Wednesday" -> Calendar.WEDNESDAY
                "Thursday" -> Calendar.THURSDAY
                "Friday" -> Calendar.FRIDAY
                "Saturday" -> Calendar.SATURDAY
                else -> return@forEach
            }

            // Cancel main alarm
            val mainIntent = Intent(this, AlarmReceiver::class.java)
            val mainPendingIntent = PendingIntent.getBroadcast(
                this,
                (medicine.id.toInt() * 10 + dayOfWeek),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(mainPendingIntent)

            // Cancel follow-up alarm
            val followUpPendingIntent = PendingIntent.getBroadcast(
                this,
                (medicine.id.toInt() * 10 + dayOfWeek + 100),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(followUpPendingIntent)
        }
    }

}
