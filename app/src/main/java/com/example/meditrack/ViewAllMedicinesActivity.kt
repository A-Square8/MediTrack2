package com.example.meditrack

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfWriter
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class ViewAllMedicinesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AllMedicineAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var btnDownloadPdf: Button
    private lateinit var tvEmpty: TextView

    // Permission launcher for Android 13+
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            generatePDF()
        } else {
            Toast.makeText(this, "Storage permissions are required to save PDF", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_all_medicines)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "All Medicines"

        initViews()
        setupRecyclerView()
        loadAllMedicines()
        setupClickListeners()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewAll)
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf)
        tvEmpty = findViewById(R.id.tvEmptyAll)
        dbHelper = DatabaseHelper(this)
    }

    private fun setupRecyclerView() {
        adapter = AllMedicineAdapter(
            medicines = emptyList(),
            onDeleteClicked = { medicine ->
                deleteMedicine(medicine)
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupClickListeners() {
        btnDownloadPdf.setOnClickListener {
            checkPermissionsAndGeneratePDF()
        }
    }

    private fun loadAllMedicines() {
        val allMedicines = dbHelper.getAllMedicines()
        adapter.updateMedicines(allMedicines)

        if (allMedicines.isNotEmpty()) {
            recyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
        } else {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        }
    }

    private fun deleteMedicine(medicine: Medicine) {
        dbHelper.deleteMedicine(medicine.id)
        loadAllMedicines()
        Toast.makeText(this, "Medicine deleted successfully", Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissionsAndGeneratePDF() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - No storage permissions needed for MediaStore
            generatePDF()
        } else {
            // Android 12 and below
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                generatePDF()
            } else {
                permissionLauncher.launch(arrayOf(permission))
            }
        }
    }

    private fun generatePDF() {
        try {
            val medicines = dbHelper.getAllMedicines()
            if (medicines.isEmpty()) {
                Toast.makeText(this, "No medicines to export", Toast.LENGTH_SHORT).show()
                return
            }

            val fileName = "MediTrack_Schedule_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"

            val outputStream: OutputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri: Uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                    ?: throw Exception("Failed to create file")

                contentResolver.openOutputStream(uri) ?: throw Exception("Failed to open output stream")
            } else {
                // Legacy approach for older Android versions
                val file = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                java.io.FileOutputStream(file)
            }

            val document = Document(PageSize.A4)
            PdfWriter.getInstance(document, outputStream)
            document.open()

            // Add title
            val titleFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD, BaseColor.BLACK)
            val title = Paragraph("Medicine Schedule Report", titleFont)
            title.alignment = Element.ALIGN_CENTER
            title.spacingAfter = 20f
            document.add(title)

            // Add generation date
            val dateFont = Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.GRAY)
            val dateText = Paragraph("Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", dateFont)
            dateText.alignment = Element.ALIGN_CENTER
            dateText.spacingAfter = 20f
            document.add(dateText)

            // Create table
            val table = PdfPTable(4)
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(3f, 1.5f, 1.5f, 3f))

            // Add table headers
            val headerFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE)
            val headerCells = arrayOf("Medicine Name", "Dose", "Time", "Days")

            headerCells.forEach { header ->
                val cell = PdfPCell(Phrase(header, headerFont))
                cell.backgroundColor = BaseColor(255, 140, 0)
                cell.horizontalAlignment = Element.ALIGN_CENTER
                cell.setPadding(8f)
                table.addCell(cell)
            }

            // Add medicine data
            val dataFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK)
            medicines.forEach { medicine ->
                val nameCell = PdfPCell(Phrase(medicine.name, dataFont))
                nameCell.setPadding(6f)
                table.addCell(nameCell)

                val doseCell = PdfPCell(Phrase(medicine.dose, dataFont))
                doseCell.setPadding(6f)
                table.addCell(doseCell)

                val timeCell = PdfPCell(Phrase(medicine.time, dataFont))
                timeCell.setPadding(6f)
                table.addCell(timeCell)

                val daysCell = PdfPCell(Phrase(medicine.days.replace(",", ", "), dataFont))
                daysCell.setPadding(6f)
                table.addCell(daysCell)
            }

            document.add(table)

            // Add footer
            val footerText = Paragraph("\n\nTotal Medicines: ${medicines.size}", dateFont)
            footerText.alignment = Element.ALIGN_CENTER
            document.add(footerText)

            document.close()
            outputStream.close()

            Toast.makeText(this, "PDF saved to Downloads folder: $fileName", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
