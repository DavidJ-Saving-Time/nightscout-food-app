package com.atelierdjames.nillafood

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "glucose_entries")
data class GlucoseEntry(
    @PrimaryKey val id: String,
    val sgv: Float,
    val direction: String?,
    val device: String?,
    val date: Long,
    val noise: Int?
)
