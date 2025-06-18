package com.example.meditrack

import android.Manifest
import android.content.ContentValues
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
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfWriter
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var tvStrikeRate: TextView
    private lateinit var tvAvgPillsPerDay: TextView
    private lateinit var tvAvgTimeDelay: TextView
    private lateinit var btnDownloadReport: Button
    private lateinit var analyticsDbHelper: AnalyticsDbHelper
    private lateinit var mainDbHelper: DatabaseHelper

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            generateDetailedReport()
        } else {
            Toast.makeText(this, "Storage permissions required for PDF generation", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Analytics"

        initViews()
        setupClickListeners()
        loadAnalytics()
    }

    override fun onResume() {
        super.onResume()
        loadAnalytics() // Refresh analytics when returning to this activity
    }

    private fun initViews() {
        tvStrikeRate = findViewById(R.id.tvStrikeRate)
        tvAvgPillsPerDay = findViewById(R.id.tvAvgPillsPerDay)
        tvAvgTimeDelay = findViewById(R.id.tvAvgTimeDelay)
        btnDownloadReport = findViewById(R.id.btnDownloadReport)

        analyticsDbHelper = AnalyticsDbHelper(this)
        mainDbHelper = DatabaseHelper(this)
    }

    private fun setupClickListeners() {
        btnDownloadReport.setOnClickListener {
            checkPermissionsAndGenerateReport()
        }
    }

    private fun loadAnalytics() {
        val analytics = analyticsDbHelper.calculateAnalytics()

        tvStrikeRate.text = "${String.format("%.1f", analytics.strikeRate)}%"
        tvAvgPillsPerDay.text = String.format("%.1f", analytics.avgPillsPerDay)
        tvAvgTimeDelay.text = "${analytics.avgTimeDelayMinutes} min"

        android.util.Log.d("AnalyticsActivity", "Analytics loaded - Strike Rate: ${analytics.strikeRate}%, Avg Medicines: ${analytics.avgPillsPerDay}, Delay: ${analytics.avgTimeDelayMinutes}")
    }

    // Rest of the methods remain the same...
    private fun checkPermissionsAndGenerateReport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            generateDetailedReport()
        } else {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                generateDetailedReport()
            } else {
                permissionLauncher.launch(arrayOf(permission))
            }
        }
    }

    private fun generateDetailedReport() {
        try {
            val fileName = "MediTrack_Analytics_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"

            val outputStream: OutputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri: Uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                    ?: throw Exception("Failed to create file")
                contentResolver.openOutputStream(uri) ?: throw Exception("Failed to open output stream")
            } else {
                val file = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                java.io.FileOutputStream(file)
            }

            createDetailedPDFReport(outputStream)
            Toast.makeText(this, "Analytics report saved to Downloads: $fileName", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error generating report: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createDetailedPDFReport(outputStream: OutputStream) {
        val document = Document(PageSize.A4)
        PdfWriter.getInstance(document, outputStream)
        document.open()

        // Title
        val titleFont = Font(Font.FontFamily.HELVETICA, 20f, Font.BOLD, BaseColor.BLACK)
        val title = Paragraph("MediTrack Analytics Report", titleFont)
        title.alignment = Element.ALIGN_CENTER
        title.spacingAfter = 20f
        document.add(title)

        // Date
        val dateFont = Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.GRAY)
        val dateText = Paragraph("Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", dateFont)
        dateText.alignment = Element.ALIGN_CENTER
        dateText.spacingAfter = 30f
        document.add(dateText)

        // Analytics Summary
        val analytics = analyticsDbHelper.calculateAnalytics()
        addAnalyticsSummary(document, analytics)

        // Detailed Medicine History
        addMedicineHistory(document)

        // Adherence Trends
        addAdherenceTrends(document)

        document.close()
        outputStream.close()
    }

    private fun addAnalyticsSummary(document: Document, analytics: AnalyticsData) {
        val headerFont = Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD, BaseColor.BLACK)
        val header = Paragraph("Analytics Summary", headerFont)
        header.spacingAfter = 15f
        document.add(header)

        val table = PdfPTable(2)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(1f, 1f))

        val cellFont = Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL, BaseColor.BLACK)

        addTableRow(table, "Strike Rate", "${String.format("%.1f", analytics.strikeRate)}%", cellFont)
        addTableRow(table, "Average Medicines Per Day", String.format("%.1f", analytics.avgPillsPerDay), cellFont)
        addTableRow(table, "Average Time Delay", "${analytics.avgTimeDelayMinutes} minutes", cellFont)
        addTableRow(table, "Total Medicines Tracked", "${analytics.totalMedicinesTracked}", cellFont)
        addTableRow(table, "Active Medicines", "${analytics.activeMedicines}", cellFont)
        addTableRow(table, "Deleted Medicines", "${analytics.deletedMedicines}", cellFont)

        document.add(table)
        document.add(Paragraph("\n"))
    }

    private fun addMedicineHistory(document: Document) {
        val headerFont = Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD, BaseColor.BLACK)
        val header = Paragraph("Medicine History", headerFont)
        header.spacingAfter = 15f
        document.add(header)

        val medicineHistory = analyticsDbHelper.getMedicineHistory()
        val table = PdfPTable(4)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(2f, 1f, 1f, 1f))

        val headerCellFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE)
        addHeaderCell(table, "Medicine Name", headerCellFont)
        addHeaderCell(table, "Total Doses", headerCellFont)
        addHeaderCell(table, "Taken", headerCellFont)
        addHeaderCell(table, "Status", headerCellFont)

        val cellFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK)
        medicineHistory.forEach { medicine ->
            addTableCell(table, medicine.name, cellFont)
            addTableCell(table, medicine.totalDoses.toString(), cellFont)
            addTableCell(table, medicine.takenDoses.toString(), cellFont)
            addTableCell(table, if (medicine.isDeleted) "Deleted" else "Active", cellFont)
        }

        document.add(table)
        document.add(Paragraph("\n"))
    }

    private fun addAdherenceTrends(document: Document) {
        val headerFont = Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD, BaseColor.BLACK)
        val header = Paragraph("Weekly Adherence Trends", headerFont)
        header.spacingAfter = 15f
        document.add(header)

        val weeklyData = analyticsDbHelper.getWeeklyAdherence()
        val table = PdfPTable(3)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(2f, 1f, 1f))

        val headerCellFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE)
        addHeaderCell(table, "Week", headerCellFont)
        addHeaderCell(table, "Scheduled", headerCellFont)
        addHeaderCell(table, "Taken", headerCellFont)

        val cellFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.BLACK)
        weeklyData.forEach { week ->
            addTableCell(table, week.weekRange, cellFont)
            addTableCell(table, week.scheduledDoses.toString(), cellFont)
            addTableCell(table, week.takenDoses.toString(), cellFont)
        }

        document.add(table)
    }

    private fun addTableRow(table: PdfPTable, label: String, value: String, font: Font) {
        val labelCell = PdfPCell(Phrase(label, font))
        labelCell.setPadding(8f)
        labelCell.backgroundColor = BaseColor.LIGHT_GRAY
        table.addCell(labelCell)

        val valueCell = PdfPCell(Phrase(value, font))
        valueCell.setPadding(8f)
        table.addCell(valueCell)
    }

    private fun addHeaderCell(table: PdfPTable, text: String, font: Font) {
        val cell = PdfPCell(Phrase(text, font))
        cell.backgroundColor = BaseColor(255, 140, 0)
        cell.horizontalAlignment = Element.ALIGN_CENTER
        cell.setPadding(8f)
        table.addCell(cell)
    }

    private fun addTableCell(table: PdfPTable, text: String, font: Font) {
        val cell = PdfPCell(Phrase(text, font))
        cell.setPadding(6f)
        table.addCell(cell)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
