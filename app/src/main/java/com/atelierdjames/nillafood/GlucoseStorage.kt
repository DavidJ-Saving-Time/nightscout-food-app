package com.atelierdjames.nillafood

import android.content.Context
import androidx.room.Room

object GlucoseStorage {
    private const val DB_NAME = "app-db"

    @Volatile
    private var INSTANCE: AppDatabase? = null

    private fun db(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            ).build().also { INSTANCE = it }
        }
    }

    suspend fun getAllEntries(context: Context): List<Pair<String, Float>> {
        return db(context).glucoseDao().getAll().map { it.timestamp to it.value }
    }

    suspend fun addEntries(context: Context, entries: List<Pair<String, Float>>) {
        if (entries.isEmpty()) return
        val list = entries.map { GlucoseEntry(it.first, it.second) }
        db(context).glucoseDao().insertAll(list)
    }

    suspend fun getLatestTimestamp(context: Context): String? {
        return db(context).glucoseDao().getLatestTimestamp()
    }
}
