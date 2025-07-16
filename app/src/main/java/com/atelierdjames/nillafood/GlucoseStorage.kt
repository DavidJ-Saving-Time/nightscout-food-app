package com.atelierdjames.nillafood

/**
 * Convenience wrapper around the [GlucoseDao] providing coroutine based helper
 * methods for reading and writing glucose entries.
 */

import android.content.Context

object GlucoseStorage {
    /** Shortcut to obtain the [AppDatabase] instance. */
    private fun db(context: Context): AppDatabase {
        return DatabaseProvider.db(context)
    }

    /** Returns all stored glucose readings. */
    suspend fun getAllEntries(context: Context): List<GlucoseEntry> {
        return db(context).glucoseDao().getAll()
    }

    /** Inserts new glucose entries if the provided list is not empty. */
    suspend fun addEntries(context: Context, entries: List<GlucoseEntry>) {
        if (entries.isEmpty()) return
        db(context).glucoseDao().insertAll(entries)
    }

    /**
     * Clears all existing entries and stores the provided ones. If the list is
     * empty the table is simply cleared.
     */
    suspend fun replaceAll(context: Context, entries: List<GlucoseEntry>) {
        val dao = db(context).glucoseDao()
        dao.deleteAll()
        if (entries.isNotEmpty()) {
            dao.insertAll(entries)
        }
    }

    /** Timestamp of the newest reading in storage. */
    suspend fun getLatestTimestamp(context: Context): Long? {
        return db(context).glucoseDao().getLatestTimestamp()
    }

    /** Timestamp of the oldest reading in storage. */
    suspend fun getEarliestTimestamp(context: Context): Long? {
        return db(context).glucoseDao().getEarliestTimestamp()
    }
}
