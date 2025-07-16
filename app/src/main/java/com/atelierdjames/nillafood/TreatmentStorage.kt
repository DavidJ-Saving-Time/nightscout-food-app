package com.atelierdjames.nillafood

/**
 * Simple facade over [TreatmentDao] providing coroutine based access methods
 * for storing and retrieving meal entries.
 */

import android.content.Context

object TreatmentStorage {
    /** Obtain the [AppDatabase] instance for Room operations. */
    private fun db(context: Context) = DatabaseProvider.db(context)

    /** Retrieve all treatments from the local database. */
    suspend fun getAll(context: Context): List<Treatment> {
        return db(context).treatmentDao().getAll().map { it.toTreatment() }
    }

    /** Insert or update the given [treatments]. */
    suspend fun addOrUpdate(context: Context, treatments: List<Treatment>) {
        val entities = treatments.mapNotNull { TreatmentEntity.from(it) }
        if (entities.isNotEmpty()) {
            db(context).treatmentDao().insertAll(entities)
        }
    }

    /** Replace all stored treatments with [treatments]. */
    suspend fun replaceAll(context: Context, treatments: List<Treatment>) {
        val dao = db(context).treatmentDao()
        dao.deleteAll()
        val entities = treatments.mapNotNull { TreatmentEntity.from(it) }
        if (entities.isNotEmpty()) {
            dao.insertAll(entities)
        }
    }

    /** Delete a treatment with the given [id]. */
    suspend fun delete(context: Context, id: String) {
        db(context).treatmentDao().deleteById(id)
    }

    /** Timestamp of the most recent meal. */
    suspend fun getLatestTimestamp(context: Context): Long? {
        return db(context).treatmentDao().getLatestTimestamp()
    }

    /** Retrieve all meals that occurred after [start]. */
    suspend fun getSince(context: Context, start: Long): List<Treatment> {
        return db(context).treatmentDao().getSince(start).map { it.toTreatment() }
    }
}
