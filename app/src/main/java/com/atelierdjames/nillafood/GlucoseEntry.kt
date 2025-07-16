package com.atelierdjames.nillafood

/**
 * Entity representing a single CGM reading retrieved from Nightscout.
 * All timestamps are stored as epoch milliseconds.
 */
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "glucose_entries")
data class GlucoseEntry(
    /** Unique identifier from the Nightscout API. */
    @PrimaryKey val id: String,
    /** Sensor glucose value in mg/dL. */
    val sgv: Float,
    /** Direction/trend as reported by the device. */
    val direction: String?,
    /** Device that produced this reading. */
    val device: String?,
    /** Time of the reading in epoch milliseconds. */
    val date: Long,
    /** Optional noise value indicating reading quality. */
    val noise: Int?,
)
