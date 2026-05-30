package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agenda_items")
data class AgendaItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val dateTime: Long = System.currentTimeMillis(),
    val priority: String = "ROUTINE",
    val isCompleted: Boolean = false,
    val location: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
