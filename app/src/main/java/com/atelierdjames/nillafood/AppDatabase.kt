package com.atelierdjames.nillafood

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GlucoseEntry::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun glucoseDao(): GlucoseDao
}
