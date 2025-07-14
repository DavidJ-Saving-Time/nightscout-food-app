package com.atelierdjames.nillafood

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "insulin_injections")
data class InsulinInjectionEntity(
    @PrimaryKey val id: String,
    val time: String,
    val insulin: String,
    val units: Float
) {
    fun toInjection(): InsulinInjection = InsulinInjection(id, time, insulin, units)

    companion object {
        fun from(injection: InsulinInjection): InsulinInjectionEntity =
            InsulinInjectionEntity(injection.id, injection.time, injection.insulin, injection.units)
    }
}
