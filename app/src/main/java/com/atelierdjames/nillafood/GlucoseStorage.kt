package com.atelierdjames.nillafood

import android.content.Context

object GlucoseStorage {
    private fun db(context: Context): AppDatabase {
        return DatabaseProvider.db(context)
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
