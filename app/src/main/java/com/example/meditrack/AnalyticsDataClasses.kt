package com.example.meditrack

data class AnalyticsData(
    val strikeRate: Double,
    val avgPillsPerDay: Double,
    val avgTimeDelayMinutes: Long,
    val totalMedicinesTracked: Int,
    val activeMedicines: Int,
    val deletedMedicines: Int
)

data class MedicineHistoryData(
    val name: String,
    val totalDoses: Int,
    val takenDoses: Int,
    val isDeleted: Boolean
)

data class WeeklyAdherenceData(
    val weekRange: String,
    val scheduledDoses: Int,
    val takenDoses: Int
)
