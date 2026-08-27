package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "away_sessions",
    indices = [
        Index(value = ["dateKey"]),
        Index(value = ["startTime"]),
        Index(value = ["isActive"])
    ]
)
data class AwaySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val durationMillis: Long = 0L,
    val dateKey: String, // Format: "yyyy-MM-dd"
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
