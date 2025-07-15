package com.atelierdjames.nillafood

import android.content.Context

object GlucoseStorage {
    private fun db(context: Context): AppDatabase {
        return DatabaseProvider.db(context)
    }

    suspend fun getAllEntries(context: Context): List<GlucoseEntry> {
        return db(context).glucoseDao().getAll()
    }

    suspend fun addEntries(context: Context, entries: List<GlucoseEntry>) {
        if (entries.isEmpty()) return
        db(context).glucoseDao().insertAll(entries)
    }

    suspend fun getLatestTimestamp(context: Context): Long? {
        return db(context).glucoseDao().getLatestTimestamp()
    }

    suspend fun getEarliestTimestamp(context: Context): Long? {
        return db(context).glucoseDao().getEarliestTimestamp()
    }
}
