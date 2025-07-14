package com.atelierdjames.nillafood

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private const val DB_NAME = "app-db"

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun db(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
