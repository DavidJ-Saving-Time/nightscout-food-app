package com.atelierdjames.nillafood

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "glucose_entries")
data class GlucoseEntry(
    @PrimaryKey val timestamp: String,
    val value: Float
)
