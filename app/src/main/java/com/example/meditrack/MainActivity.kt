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
        recyclerView = findViewById(R.id.recyclerView)

        setupRecyclerView()
        loadMedicines()

        // Reset daily checkboxes at app start (new day)
        checkAndResetDailyCheckboxes()

        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddMedicineActivity::class.java))
        }

        btnView.setOnClickListener {
            startActivity(Intent(this, ViewAllMedicinesActivity::class.java))
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
                    cancelFollowUpAlarm(medicine)
                }
            },
            onDeleteClicked = { medicine ->
                cancelMedicineAlarm(medicine)
                dbHelper.deleteMedicine(medicine.id)
                loadMedicines()
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun cancelFollowUpAlarm(medicine: Medicine) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("medicine_name", medicine.name)
            putExtra("dose", medicine.dose)
            putExtra("type", "followup")
            putExtra("medicine_id", medicine.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            (medicine.id + 100000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun loadMedicines() {
        val allMedicines = dbHelper.getAllMedicines()
        val today = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(java.util.Date())
        val todayMedicines = allMedicines.filter { medicine ->
            medicine.days.split(",").map { it.trim() }.contains(today)
        }

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
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("medicine_name", medicine.name)
            putExtra("dose", medicine.dose)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            medicine.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
