package com.example.meditrack

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsDbHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "MediTrackAnalytics.db"
        private const val DATABASE_VERSION = 2 // Increased version to force recreation

        // Medicine Logs Table
        private const val TABLE_MEDICINE_LOGS = "medicine_logs"
        private const val COLUMN_LOG_ID = "log_id"
        private const val COLUMN_MEDICINE_ID = "medicine_id"
        private const val COLUMN_MEDICINE_NAME = "medicine_name"
        private const val COLUMN_SCHEDULED_TIME = "scheduled_time"
        private const val COLUMN_ACTUAL_TIME = "actual_time"
        private const val COLUMN_DOSE = "dose"
        private const val COLUMN_WAS_TAKEN = "was_taken"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_IS_DELETED = "is_deleted"

        // Deleted Medicines Table
        private const val TABLE_DELETED_MEDICINES = "deleted_medicines"
        private const val COLUMN_DELETED_ID = "deleted_id"
        private const val COLUMN_DELETED_NAME = "deleted_name"
        private const val COLUMN_DELETED_DOSE = "deleted_dose"
        private const val COLUMN_DELETED_TIME = "deleted_time"
        private const val COLUMN_DELETED_DAYS = "deleted_days"
        private const val COLUMN_DELETION_DATE = "deletion_date"
        private const val COLUMN_TOTAL_LOGS = "total_logs"
        private const val COLUMN_TAKEN_LOGS = "taken_logs"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createMedicineLogsTable = """
            CREATE TABLE $TABLE_MEDICINE_LOGS (
                $COLUMN_LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_MEDICINE_ID INTEGER NOT NULL,
                $COLUMN_MEDICINE_NAME TEXT NOT NULL,
                $COLUMN_SCHEDULED_TIME TEXT NOT NULL,
                $COLUMN_ACTUAL_TIME TEXT,
                $COLUMN_DOSE TEXT NOT NULL,
                $COLUMN_WAS_TAKEN INTEGER DEFAULT 0,
                $COLUMN_DATE TEXT NOT NULL,
                $COLUMN_IS_DELETED INTEGER DEFAULT 0
            )
        """.trimIndent()

        val createDeletedMedicinesTable = """
            CREATE TABLE $TABLE_DELETED_MEDICINES (
                $COLUMN_DELETED_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DELETED_NAME TEXT NOT NULL,
                $COLUMN_DELETED_DOSE TEXT NOT NULL,
                $COLUMN_DELETED_TIME TEXT NOT NULL,
                $COLUMN_DELETED_DAYS TEXT NOT NULL,
                $COLUMN_DELETION_DATE TEXT NOT NULL,
                $COLUMN_TOTAL_LOGS INTEGER DEFAULT 0,
                $COLUMN_TAKEN_LOGS INTEGER DEFAULT 0
            )
        """.trimIndent()

        db?.execSQL(createMedicineLogsTable)
        db?.execSQL(createDeletedMedicinesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_MEDICINE_LOGS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_DELETED_MEDICINES")
        onCreate(db)
    }

    fun logMedicineSchedule(medicine: Medicine, date: String) {
        val db = writableDatabase

        // Check if already logged for this medicine and date
        val existingCursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_MEDICINE_LOGS WHERE $COLUMN_MEDICINE_ID = ? AND $COLUMN_DATE = ?",
            arrayOf(medicine.id.toString(), date)
        )

        var exists = false
        if (existingCursor.moveToFirst()) {
            exists = existingCursor.getInt(0) > 0
        }
        existingCursor.close()

        if (!exists) {
            val values = ContentValues().apply {
                put(COLUMN_MEDICINE_ID, medicine.id)
                put(COLUMN_MEDICINE_NAME, medicine.name)
                put(COLUMN_SCHEDULED_TIME, medicine.time)
                put(COLUMN_DOSE, medicine.dose)
                put(COLUMN_DATE, date)
                put(COLUMN_WAS_TAKEN, 0)
                put(COLUMN_IS_DELETED, 0)
            }
            val result = db.insert(TABLE_MEDICINE_LOGS, null, values)
            android.util.Log.d("AnalyticsDbHelper", "Logged medicine schedule: ${medicine.name}, Result: $result")
        }
        db.close()
    }

    fun logMedicineTaken(medicineId: Long, actualTime: String, date: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ACTUAL_TIME, actualTime)
            put(COLUMN_WAS_TAKEN, 1)
        }
        val rowsUpdated = db.update(TABLE_MEDICINE_LOGS, values,
            "$COLUMN_MEDICINE_ID = ? AND $COLUMN_DATE = ?",
            arrayOf(medicineId.toString(), date))
        android.util.Log.d("AnalyticsDbHelper", "Medicine taken logged: ID=$medicineId, Rows updated: $rowsUpdated")
        db.close()
    }

    fun logDeletedMedicine(medicine: Medicine) {
        val db = writableDatabase

        // Get statistics for this medicine
        val cursor = db.rawQuery(
            "SELECT COUNT(*) as total, SUM($COLUMN_WAS_TAKEN) as taken FROM $TABLE_MEDICINE_LOGS WHERE $COLUMN_MEDICINE_ID = ?",
            arrayOf(medicine.id.toString())
        )

        var totalLogs = 0
        var takenLogs = 0
        if (cursor.moveToFirst()) {
            totalLogs = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
            takenLogs = cursor.getInt(cursor.getColumnIndexOrThrow("taken"))
        }
        cursor.close()

        // Insert into deleted medicines table
        val values = ContentValues().apply {
            put(COLUMN_DELETED_NAME, medicine.name)
            put(COLUMN_DELETED_DOSE, medicine.dose)
            put(COLUMN_DELETED_TIME, medicine.time)
            put(COLUMN_DELETED_DAYS, medicine.days)
            put(COLUMN_DELETION_DATE, SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
            put(COLUMN_TOTAL_LOGS, totalLogs)
            put(COLUMN_TAKEN_LOGS, takenLogs)
        }
        db.insert(TABLE_DELETED_MEDICINES, null, values)

        // Mark existing logs as deleted
        val updateValues = ContentValues().apply {
            put(COLUMN_IS_DELETED, 1)
        }
        db.update(TABLE_MEDICINE_LOGS, updateValues, "$COLUMN_MEDICINE_ID = ?", arrayOf(medicine.id.toString()))

        db.close()
    }

    // Helper function to convert dose string to double
    private fun parseDose(doseString: String): Double {
        return when (doseString) {
            "1/4" -> 0.25
            "1/2" -> 0.5
            else -> doseString.toDoubleOrNull() ?: 1.0
        }
    }

    fun calculateAnalytics(): AnalyticsData {
        val db = readableDatabase

        // Debug: Check total records
        val totalCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_MEDICINE_LOGS", null)
        var totalRecords = 0
        if (totalCursor.moveToFirst()) {
            totalRecords = totalCursor.getInt(0)
        }
        totalCursor.close()
        android.util.Log.d("AnalyticsDbHelper", "Total records in analytics DB: $totalRecords")

        // Calculate strike rate with proper dose parsing
        val strikeRateCursor = db.rawQuery(
            "SELECT $COLUMN_DOSE, $COLUMN_WAS_TAKEN FROM $TABLE_MEDICINE_LOGS WHERE $COLUMN_IS_DELETED = 0",
            null
        )

        var totalScheduledPills = 0.0
        var totalTakenPills = 0.0

        if (strikeRateCursor.moveToFirst()) {
            do {
                val doseString = strikeRateCursor.getString(strikeRateCursor.getColumnIndexOrThrow(COLUMN_DOSE))
                val wasTaken = strikeRateCursor.getInt(strikeRateCursor.getColumnIndexOrThrow(COLUMN_WAS_TAKEN))
                val doseValue = parseDose(doseString)

                totalScheduledPills += doseValue
                if (wasTaken == 1) {
                    totalTakenPills += doseValue
                }
            } while (strikeRateCursor.moveToNext())
        }
        strikeRateCursor.close()

        val strikeRate = if (totalScheduledPills > 0) (totalTakenPills / totalScheduledPills) * 100 else 0.0
        android.util.Log.d("AnalyticsDbHelper", "Strike Rate: $strikeRate% (Taken: $totalTakenPills, Scheduled: $totalScheduledPills)")

        // Calculate average medicines per day (scheduled medicines, not pills)
        val medicinesCursor = db.rawQuery(
            "SELECT $COLUMN_DATE, COUNT(DISTINCT $COLUMN_MEDICINE_ID) as medicine_count FROM $TABLE_MEDICINE_LOGS WHERE $COLUMN_IS_DELETED = 0 GROUP BY $COLUMN_DATE",
            null
        )

        val dailyMedicineCounts = mutableListOf<Int>()

        if (medicinesCursor.moveToFirst()) {
            do {
                val medicineCount = medicinesCursor.getInt(medicinesCursor.getColumnIndexOrThrow("medicine_count"))
                dailyMedicineCounts.add(medicineCount)
            } while (medicinesCursor.moveToNext())
        }
        medicinesCursor.close()

        val avgMedicinesPerDay = if (dailyMedicineCounts.isNotEmpty()) {
            dailyMedicineCounts.average()
        } else 0.0

        android.util.Log.d("AnalyticsDbHelper", "Average medicines per day: $avgMedicinesPerDay")

        // Fixed time delay calculation
        val delayCursor = db.rawQuery(
            "SELECT $COLUMN_SCHEDULED_TIME, $COLUMN_ACTUAL_TIME FROM $TABLE_MEDICINE_LOGS WHERE $COLUMN_WAS_TAKEN = 1 AND $COLUMN_ACTUAL_TIME IS NOT NULL AND $COLUMN_IS_DELETED = 0",
            null
        )

        var totalDelayMinutes = 0L
        var delayCount = 0

        if (delayCursor.moveToFirst()) {
            do {
                val scheduledTime = delayCursor.getString(delayCursor.getColumnIndexOrThrow(COLUMN_SCHEDULED_TIME))
                val actualTime = delayCursor.getString(delayCursor.getColumnIndexOrThrow(COLUMN_ACTUAL_TIME))

                val delay = calculateTimeDelayFixed(scheduledTime, actualTime)
                // Only count positive delays (late taking)
                if (delay > 0) {
                    totalDelayMinutes += delay
                    delayCount++
                }
            } while (delayCursor.moveToNext())
        }
        delayCursor.close()

        val avgTimeDelayMinutes = if (delayCount > 0) totalDelayMinutes / delayCount else 0L

        // Get medicine counts
        val mainDbHelper = DatabaseHelper(context)
        val activeMedicines = mainDbHelper.getAllMedicines().size

        val deletedCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_DELETED_MEDICINES", null)
        var deletedMedicines = 0
        if (deletedCursor.moveToFirst()) {
            deletedMedicines = deletedCursor.getInt(0)
        }
        deletedCursor.close()

        db.close()

        return AnalyticsData(
            strikeRate = strikeRate,
            avgPillsPerDay = avgMedicinesPerDay, // This is now average medicines per day
            avgTimeDelayMinutes = avgTimeDelayMinutes,
            totalMedicinesTracked = activeMedicines + deletedMedicines,
            activeMedicines = activeMedicines,
            deletedMedicines = deletedMedicines
        )
    }

    private fun calculateTimeDelayFixed(scheduledTime: String, actualTime: String): Long {
        return try {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val scheduled = format.parse(scheduledTime)
            val actual = format.parse(actualTime)

            if (scheduled != null && actual != null) {
                var diffMs = actual.time - scheduled.time

                // Handle case where actual time is next day (e.g., scheduled 23:30, actual 00:30)
                if (diffMs < -12 * 60 * 60 * 1000) { // More than 12 hours early means next day
                    diffMs += 24 * 60 * 60 * 1000
                }

                diffMs / (1000 * 60) // Convert to minutes
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun getMedicineHistory(): List<MedicineHistoryData> {
        val db = readableDatabase
        val history = mutableListOf<MedicineHistoryData>()

        // Get active medicines
        val activeCursor = db.rawQuery(
            "SELECT $COLUMN_MEDICINE_NAME, COUNT(*) as total, SUM($COLUMN_WAS_TAKEN) as taken FROM $TABLE_MEDICINE_LOGS WHERE $COLUMN_IS_DELETED = 0 GROUP BY $COLUMN_MEDICINE_NAME",
            null
        )

        if (activeCursor.moveToFirst()) {
            do {
                history.add(MedicineHistoryData(
                    name = activeCursor.getString(activeCursor.getColumnIndexOrThrow(COLUMN_MEDICINE_NAME)),
                    totalDoses = activeCursor.getInt(activeCursor.getColumnIndexOrThrow("total")),
                    takenDoses = activeCursor.getInt(activeCursor.getColumnIndexOrThrow("taken")),
                    isDeleted = false
                ))
            } while (activeCursor.moveToNext())
        }
        activeCursor.close()

        // Get deleted medicines
        val deletedCursor = db.rawQuery(
            "SELECT $COLUMN_DELETED_NAME, $COLUMN_TOTAL_LOGS, $COLUMN_TAKEN_LOGS FROM $TABLE_DELETED_MEDICINES",
            null
        )

        if (deletedCursor.moveToFirst()) {
            do {
                history.add(MedicineHistoryData(
                    name = deletedCursor.getString(deletedCursor.getColumnIndexOrThrow(COLUMN_DELETED_NAME)),
                    totalDoses = deletedCursor.getInt(deletedCursor.getColumnIndexOrThrow(COLUMN_TOTAL_LOGS)),
                    takenDoses = deletedCursor.getInt(deletedCursor.getColumnIndexOrThrow(COLUMN_TAKEN_LOGS)),
                    isDeleted = true
                ))
            } while (deletedCursor.moveToNext())
        }
        deletedCursor.close()

        db.close()
        return history
    }

    fun getWeeklyAdherence(): List<WeeklyAdherenceData> {
        val db = readableDatabase
        val weeklyData = mutableListOf<WeeklyAdherenceData>()

        val cursor = db.rawQuery(
            """
            SELECT 
                strftime('%Y-%W', $COLUMN_DATE) as week,
                COUNT(*) as scheduled,
                SUM($COLUMN_WAS_TAKEN) as taken
            FROM $TABLE_MEDICINE_LOGS 
            WHERE $COLUMN_IS_DELETED = 0
            GROUP BY strftime('%Y-%W', $COLUMN_DATE)
            ORDER BY week DESC
            LIMIT 8
            """.trimIndent(),
            null
        )

        if (cursor.moveToFirst()) {
            do {
                val week = cursor.getString(cursor.getColumnIndexOrThrow("week"))
                val scheduled = cursor.getInt(cursor.getColumnIndexOrThrow("scheduled"))
                val taken = cursor.getInt(cursor.getColumnIndexOrThrow("taken"))

                weeklyData.add(WeeklyAdherenceData(
                    weekRange = "Week $week",
                    scheduledDoses = scheduled,
                    takenDoses = taken
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        return weeklyData
    }

    // Debug function to check data
    fun debugPrintData() {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MEDICINE_LOGS", null)

        android.util.Log.d("AnalyticsDbHelper", "=== DEBUG: All Medicine Logs ===")
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MEDICINE_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDICINE_NAME))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))
                val taken = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WAS_TAKEN))
                val dose = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DOSE))

                android.util.Log.d("AnalyticsDbHelper", "ID: $id, Name: $name, Date: $date, Taken: $taken, Dose: $dose")
            } while (cursor.moveToNext())
        } else {
            android.util.Log.d("AnalyticsDbHelper", "No records found!")
        }
        cursor.close()
        db.close()
    }
}
