package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "office_items")
data class OfficeItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // "SPREADSHEET", "REPORT", "TEXT", "MINUTE"
    val content: String, // Text contents or CSV contents
    val csvData: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)
