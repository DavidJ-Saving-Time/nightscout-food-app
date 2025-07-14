package com.atelierdjames.nillafood

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "treatments")
data class TreatmentEntity(
    @PrimaryKey val id: String,
    val carbs: Float,
    val protein: Float,
    val fat: Float,
    val note: String,
    val timestamp: Long
) {
    fun toTreatment(): Treatment = Treatment(carbs, protein, fat, note, timestamp, id)

    companion object {
        fun from(treatment: Treatment): TreatmentEntity? {
            val tid = treatment.id ?: return null
            return TreatmentEntity(tid, treatment.carbs, treatment.protein, treatment.fat, treatment.note, treatment.timestamp)
        }
    }
}
