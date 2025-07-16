package com.atelierdjames.nillafood

/**
 * Central Room database for the Nilla Food application.
 * It defines the entities that are stored locally and provides
 * Data Access Objects (DAOs) used throughout the app.
 */

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GlucoseEntry::class, TreatmentEntity::class, InsulinInjectionEntity::class],
    version = 6
)
abstract class AppDatabase : RoomDatabase() {
    /** Returns the DAO used to manage glucose readings. */
    abstract fun glucoseDao(): GlucoseDao

    /** Returns the DAO used to manage meal treatments. */
    abstract fun treatmentDao(): TreatmentDao

    /** Returns the DAO used to manage insulin injections. */
    abstract fun insulinDao(): InsulinInjectionDao
}
