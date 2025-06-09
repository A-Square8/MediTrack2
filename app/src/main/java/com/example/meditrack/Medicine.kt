package com.example.meditrack

data class Medicine(
    val id: Long = 0,
    val name: String,
    val dose: String,
    val time: String,
    val days: String, // comma-separated days
    var isConsumed: Boolean = false
)