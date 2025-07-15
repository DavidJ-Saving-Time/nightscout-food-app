package com.atelierdjames.nillafood

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GlucoseEntry::class, TreatmentEntity::class, InsulinInjectionEntity::class],
    version = 6
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun glucoseDao(): GlucoseDao
    abstract fun treatmentDao(): TreatmentDao
    abstract fun insulinDao(): InsulinInjectionDao
}
