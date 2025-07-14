package com.atelierdjames.nillafood

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GlucoseEntry::class, TreatmentEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun glucoseDao(): GlucoseDao
    abstract fun treatmentDao(): TreatmentDao
}
