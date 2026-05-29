package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class Match(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val group: String,
    val team1: String,
    val team2: String,
    val date: String,
    val timeET: String,
    val venue: String,
    val stage: String?,
    // Live scores data
    val team1Score: Int? = null,
    val team2Score: Int? = null,
    val status: String = "UPCOMING", // "UPCOMING", "LIVE", "FINISHED"
    val minute: String? = null,
    val isFavorite: Boolean = false
) {
    fun toCstTime(): CstTime {
        val parts = timeET.split(":")
        if (parts.size < 2) return CstTime(timeET, "AM")
        val h = parts[0].toIntOrNull() ?: return CstTime(timeET, "AM")
        val m = parts[1].toIntOrNull() ?: 0
        var cstH = h - 1
        if (cstH < 0) cstH += 24
        val ampm = if (cstH >= 12) "PM" else "AM"
        val displayH = if (cstH % 12 == 0) 12 else cstH % 12
        val displayM = String.format("%02d", m)
        return CstTime("$displayH:$displayM", ampm)
    }
}

data class CstTime(
    val display: String,
    val ampm: String
)
