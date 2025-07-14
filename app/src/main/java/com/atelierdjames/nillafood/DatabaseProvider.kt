package com.atelierdjames.nillafood

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
            ).addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE insulin_injections RENAME TO temp_insulin")
            db.execSQL("CREATE TABLE insulin_injections (id TEXT NOT NULL, time INTEGER NOT NULL, insulin TEXT NOT NULL, units REAL NOT NULL, PRIMARY KEY(id))")
            db.execSQL("INSERT INTO insulin_injections (id, time, insulin, units) SELECT id, CAST(strftime('%s', time) * 1000 AS INTEGER), insulin, units FROM temp_insulin")
            db.execSQL("DROP TABLE temp_insulin")

            db.execSQL("ALTER TABLE treatments RENAME TO temp_treatments")
            db.execSQL("CREATE TABLE treatments (id TEXT NOT NULL, carbs REAL NOT NULL, protein REAL NOT NULL, fat REAL NOT NULL, note TEXT NOT NULL, timestamp INTEGER NOT NULL, PRIMARY KEY(id))")
            db.execSQL("INSERT INTO treatments (id, carbs, protein, fat, note, timestamp) SELECT id, carbs, protein, fat, note, CAST(strftime('%s', timestamp) * 1000 AS INTEGER) FROM temp_treatments")
            db.execSQL("DROP TABLE temp_treatments")
        }
    }
}
