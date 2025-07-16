package com.atelierdjames.nillafood

/** Room entity representing a logged meal treatment. */

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "treatments")
data class TreatmentEntity(
    /** Identifier provided by Nightscout. */
    @PrimaryKey val id: String,
    /** Carbohydrates in grams. */
    val carbs: Float,
    /** Protein in grams. */
    val protein: Float,
    /** Fat in grams. */
    val fat: Float,
    /** User entered note. */
    val note: String,
    /** When the meal occurred in epoch milliseconds. */
    val timestamp: Long,
) {
    /** Convert this entity back to the plain [Treatment] model. */
    fun toTreatment(): Treatment = Treatment(carbs, protein, fat, note, timestamp, id)

    companion object {
        /** Create a [TreatmentEntity] from a [Treatment] if it has an id. */
        fun from(treatment: Treatment): TreatmentEntity? {
            val tid = treatment.id ?: return null
            return TreatmentEntity(tid, treatment.carbs, treatment.protein, treatment.fat, treatment.note, treatment.timestamp)
        }
    }
}
