package com.atelierdjames.nillafood

import androidx.room.Entity

@Entity(tableName = "insulin_injections", primaryKeys = ["time", "insulin"])
data class InsulinInjectionEntity(
    val time: String,
    val insulin: String,
    val units: Float
) {
    fun toInjection(): InsulinInjection = InsulinInjection(time, insulin, units)

    companion object {
        fun from(injection: InsulinInjection): InsulinInjectionEntity =
            InsulinInjectionEntity(injection.time, injection.insulin, injection.units)
    }
}
