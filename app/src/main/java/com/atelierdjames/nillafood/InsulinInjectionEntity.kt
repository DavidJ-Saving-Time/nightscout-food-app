package com.atelierdjames.nillafood

/** Room entity mirroring an insulin injection entry. */

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "insulin_injections")
data class InsulinInjectionEntity(
    /** Primary key used for syncing with Nightscout. */
    @PrimaryKey val id: String,
    /** Time of the injection in epoch milliseconds. */
    val time: Long,
    /** Insulin name such as "Novorapid" or "Tresiba". */
    val insulin: String,
    /** Number of units delivered. */
    val units: Float,
) {
    /** Convert this entity into the plain [InsulinInjection] model. */
    fun toInjection(): InsulinInjection = InsulinInjection(id, time, insulin, units)

    companion object {
        /** Create an entity from the in-memory injection model. */
        fun from(injection: InsulinInjection): InsulinInjectionEntity =
            InsulinInjectionEntity(injection.id, injection.time, injection.insulin, injection.units)
    }
}
