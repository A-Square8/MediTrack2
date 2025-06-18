package com.example.meditrack

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class AddMedicineActivity : AppCompatActivity() {

    private lateinit var etMedicineName: EditText
    private lateinit var spinnerDose: Spinner
    private lateinit var btnTime: Button
    private lateinit var cbDaily: CheckBox
    private lateinit var cbSun: CheckBox
    private lateinit var cbMon: CheckBox
    private lateinit var cbTue: CheckBox
    private lateinit var cbWed: CheckBox
    private lateinit var cbThu: CheckBox
    private lateinit var cbFri: CheckBox
    private lateinit var cbSat: CheckBox
    private lateinit var btnSubmit: Button
    private lateinit var dbHelper: DatabaseHelper

    private var selectedTime = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine)

        initViews()
        setupSpinner()
        setupClickListeners()

        dbHelper = DatabaseHelper(this)
    }

    private fun initViews() {
        etMedicineName = findViewById(R.id.etMedicineName)
        spinnerDose = findViewById(R.id.spinnerDose)
        btnTime = findViewById(R.id.btnTime)
        cbDaily = findViewById(R.id.cbDaily)
        cbSun = findViewById(R.id.cbSun)
        cbMon = findViewById(R.id.cbMon)
        cbTue = findViewById(R.id.cbTue)
        cbWed = findViewById(R.id.cbWed)
        cbThu = findViewById(R.id.cbThu)
        cbFri = findViewById(R.id.cbFri)
        cbSat = findViewById(R.id.cbSat)
        btnSubmit = findViewById(R.id.btnSubmit)
    }

    private fun setupSpinner() {
        val doses = arrayOf("1/4", "1/2", "1", "2", "3", "4", "5", "6")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, doses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDose.adapter = adapter
    }

    private fun setupClickListeners() {
        btnTime.setOnClickListener {
            showTimePicker()
        }

        cbDaily.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cbSun.isChecked = true
                cbMon.isChecked = true
                cbTue.isChecked = true
                cbWed.isChecked = true
                cbThu.isChecked = true
                cbFri.isChecked = true
                cbSat.isChecked = true
            }
        }

        btnSubmit.setOnClickListener {
            saveMedicine()
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            btnTime.text = selectedTime
        }, hour, minute, true).show()
    }

    private fun saveMedicine() {
        val name = etMedicineName.text.toString().trim()
        val dose = spinnerDose.selectedItem.toString()

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter medicine name", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select time", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedDays = getSelectedDays()
        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show()
            return
        }

        val medicine = Medicine(
            name = name,
            dose = dose,
            time = selectedTime,
            days = selectedDays.joinToString(",")
        )

        val medicineId = dbHelper.addMedicine(medicine)

        if (medicineId > 0) {
            setupAlarms(medicine.copy(id = medicineId))
            setupDailyReset()  // Setup daily reset alarm
            Toast.makeText(this, "Medicine added successfully", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Failed to add medicine", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSelectedDays(): List<String> {
        val days = mutableListOf<String>()
        if (cbSun.isChecked) days.add("Sunday")
        if (cbMon.isChecked) days.add("Monday")
        if (cbTue.isChecked) days.add("Tuesday")
        if (cbWed.isChecked) days.add("Wednesday")
        if (cbThu.isChecked) days.add("Thursday")
        if (cbFri.isChecked) days.add("Friday")
        if (cbSat.isChecked) days.add("Saturday")
        return days
    }

    private fun setupAlarms(medicine: Medicine) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val timeParts = medicine.time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val selectedDays = medicine.days.split(",").map { it.trim() }

        // Set alarms for each selected day
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

            // Main reminder alarm (30 minutes before)
            val calendarBefore = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute - 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If the time has passed this week, schedule for next week
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            val intentBefore = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("medicine_name", medicine.name)
                putExtra("dose", medicine.dose)
                putExtra("type", "reminder")
                putExtra("medicine_id", medicine.id)
                putExtra("day_of_week", dayOfWeek)
            }

            val pendingIntentBefore = PendingIntent.getBroadcast(
                this,
                (medicine.id.toInt() * 10 + dayOfWeek), // Unique ID per day
                intentBefore,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendarBefore.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7, // Weekly repeat
                pendingIntentBefore
            )

            // Follow-up alarm (30 minutes after)
            val calendarFollowUp = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute + 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            val intentFollowUp = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("medicine_name", medicine.name)
                putExtra("dose", medicine.dose)
                putExtra("type", "followup")
                putExtra("medicine_id", medicine.id)
                putExtra("day_of_week", dayOfWeek)
            }

            val pendingIntentFollowUp = PendingIntent.getBroadcast(
                this,
                (medicine.id.toInt() * 10 + dayOfWeek + 100), // Unique ID for follow-up
                intentFollowUp,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendarFollowUp.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntentFollowUp
            )
        }
    }



    private fun setupDailyReset() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            // If it's already past midnight today, set for tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val intent = Intent(this, DailyResetReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            999999, // Unique ID for daily reset
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set repeating alarm for daily reset at midnight
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            24 * 60 * 60 * 1000, // 24 hours
            pendingIntent
        )
    }
}