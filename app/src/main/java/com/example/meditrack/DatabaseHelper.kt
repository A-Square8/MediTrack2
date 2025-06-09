package com.example.meditrack


import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "MediTrack.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_MEDICINES = "medicines"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_DOSE = "dose"
        private const val COLUMN_TIME = "time"
        private const val COLUMN_DAYS = "days"
        private const val COLUMN_IS_CONSUMED = "is_consumed"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """
            CREATE TABLE $TABLE_MEDICINES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_DOSE TEXT NOT NULL,
                $COLUMN_TIME TEXT NOT NULL,
                $COLUMN_DAYS TEXT NOT NULL,
                $COLUMN_IS_CONSUMED INTEGER DEFAULT 0
            )
        """.trimIndent()
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_MEDICINES")
        onCreate(db)
    }

    fun addMedicine(medicine: Medicine): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, medicine.name)
            put(COLUMN_DOSE, medicine.dose)
            put(COLUMN_TIME, medicine.time)
            put(COLUMN_DAYS, medicine.days)
            put(COLUMN_IS_CONSUMED, if (medicine.isConsumed) 1 else 0)
        }
        val id = db.insert(TABLE_MEDICINES, null, values)
        db.close()
        return id
    }

    fun getAllMedicines(): List<Medicine> {
        val medicines = mutableListOf<Medicine>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MEDICINES", null)

        if (cursor.moveToFirst()) {
            do {
                val medicine = Medicine(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    dose = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DOSE)),
                    time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME)),
                    days = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAYS)),
                    isConsumed = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_CONSUMED)) == 1
                )
                medicines.add(medicine)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return medicines
    }

    fun updateMedicineStatus(id: Long, isConsumed: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_CONSUMED, if (isConsumed) 1 else 0)
        }
        db.update(TABLE_MEDICINES, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun deleteMedicine(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_MEDICINES, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun resetDailyCheckboxes() {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_CONSUMED, 0)
        }
        db.update(TABLE_MEDICINES, values, null, null)
        db.close()
    }

}